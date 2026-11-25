package com.tolmol

import com.tolmol.user.service.*
import com.tolmol.user.domain.model.Role
import com.tolmol.user.domain.exception.InvalidCredentialsException
import com.tolmol.platform.domain.exception.BusinessException
import com.tolmol.platform.infra.config.Databases
import com.tolmol.user.api.controllers.GoogleAuthController
import com.tolmol.user.infra.security.GoogleSignInRateLimit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mock.web.*
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID

class GoogleAuthTest : BackendTest() {
    @Autowired lateinit var users: UserStore
    @Autowired lateinit var auth: AuthService
    @Autowired lateinit var issuer: TokenIssuer
    @Autowired lateinit var decoder: JwtDecoder
    @Autowired lateinit var db: Databases
    @Autowired lateinit var redis: StringRedisTemplate

    private fun identity() = GoogleIdentity(UUID.randomUUID().toString(),"google-${UUID.randomUUID()}@gmail.com","Recycler")
    private fun service(identity: GoogleIdentity) = GoogleAuthService(GoogleIdentityVerifier { token ->
        if (token != "verified-id-token") throw BusinessException(401,"Invalid Google token")
        identity
    }, users, issuer)

    @Test fun endpointCreatesVerifiedPasswordlessUserAndReusesSubject() {
        val identity = identity()
        val service = service(identity)
        MockMvcBuilders.standaloneSetup(GoogleAuthController(service)).build()
            .perform(post("/auth/google").contentType("application/json").content("""{"idToken":"verified-id-token"}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.accessToken").isString)
        val id = UUID.fromString(decoder.decode(service.login("verified-id-token")).subject)
        assertTrue(users.hasRole(id,Role.BUYER))
        assertTrue(users.hasRole(id,Role.SELLER))
        assertNull(users.find(identity.email)!!.passwordHash)
        assertTrue(db.users.sql.sql("select email_verified from accounts where id=?").param(id).query(Boolean::class.java).single())
        assertEquals(identity.subject,db.users.sql.sql("select google_subject from accounts where id=?").param(id).query(String::class.java).single())
        assertThrows(InvalidCredentialsException::class.java) { auth.login(identity.email,"some-password") }
    }

    @Test fun verifiedExistingEmailLinksWithoutChangingPasswordRolesOrProfile() {
        val identity = identity()
        val id = auth.register(identity.email,"existing-password",setOf(Role.SELLER),"Original name")
        assertEquals(id.toString(),decoder.decode(service(identity).login("verified-id-token")).subject)
        assertEquals(id.toString(),decoder.decode(auth.login(identity.email,"existing-password")).subject)
        assertEquals(setOf(Role.SELLER),auth.roles(id))
        assertEquals("Original name",db.users.sql.sql("select name from account_profiles where user_id=?").param(id).query(String::class.java).single())
        val conflicting = identity.copy(subject = UUID.randomUUID().toString())
        assertThrows(BusinessException::class.java) { service(conflicting).login("verified-id-token") }
    }

    @Test fun invalidTokenCannotCreateOrLinkAccount() {
        val identity = identity()
        assertThrows(BusinessException::class.java) { service(identity).login("invalid") }
        assertNull(users.find(identity.email))
    }

    @Test fun googleEndpointLimitsAttemptsByPeerIp() {
        val filter = GoogleSignInRateLimit(redis)
        val peer = "test-${UUID.randomUUID()}"
        repeat(31) { index ->
            val request = MockHttpServletRequest("POST","/auth/google").apply {
                servletPath = "/auth/google"; remoteAddr = peer
                addHeader("X-Forwarded-For","spoofed-$index")
            }
            val response = MockHttpServletResponse()
            filter.doFilter(request,response,MockFilterChain())
            assertEquals(if (index < 30) 200 else 429,response.status)
            if (index == 30) assertEquals("60",response.getHeader("Retry-After"))
        }
    }
}
