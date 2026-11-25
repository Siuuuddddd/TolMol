package com.tolmol

import com.tolmol.platform.infra.config.Databases
import com.tolmol.user.service.*
import com.tolmol.user.domain.model.Role
import com.tolmol.user.domain.exception.EmailTakenException
import com.tolmol.platform.domain.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import java.util.UUID

class SocialAuthTest : BackendTest() {
    @Autowired lateinit var users: UserStore
    @Autowired lateinit var auth: AuthService
    @Autowired lateinit var encoder: PasswordEncoder
    @Autowired lateinit var issuer: TokenIssuer
    @Autowired lateinit var decoder: JwtDecoder
    @Autowired lateinit var db: Databases

    private fun service(identity: SocialIdentity) = SocialAuthService(object : SocialIdentityVerifier {
        override fun verify(provider: String, token: String): SocialIdentity = identity
    }, users, encoder, issuer)

    @Test fun repeatedProviderLoginReusesAccountAndPersistsProfile() {
        val identity = SocialIdentity("GOOGLE",UUID.randomUUID().toString(),"social-${UUID.randomUUID()}@example.org","Test recycler")
        val service = service(identity)
        val subject = decoder.decode(service.login("GOOGLE","verified-test-token")).subject
        assertEquals(subject,decoder.decode(service.login("GOOGLE","another-verified-token")).subject)
        val id = UUID.fromString(subject)
        assertTrue(users.hasRole(id,Role.BUYER))
        assertTrue(users.hasRole(id,Role.SELLER))
        assertEquals("Test recycler",db.users.sql.sql("select name from account_profiles where user_id=?").param(id).query(String::class.java).single())
    }

    @Test fun socialEmailCollisionNeverLinksExistingPasswordAccount() {
        val email = "existing-${UUID.randomUUID()}@example.org"
        auth.register(email,"a-test-password",setOf(Role.BUYER))
        val identity = SocialIdentity("FACEBOOK",UUID.randomUUID().toString(),email,"Another identity")
        assertThrows(EmailTakenException::class.java) { service(identity).login("FACEBOOK","verified-test-token") }
        assertEquals(0L,db.users.sql.sql("select count(*) from social_identities where subject=?").param(identity.subject).query(Long::class.java).single())
    }

    @Test fun rejectsEmptyTokensAndMissingEmailBeforeCreatingAccount() {
        val service = service(SocialIdentity("GOOGLE","invalid-subject","","Test"))
        assertThrows(IllegalArgumentException::class.java) { service.login("GOOGLE","") }
        assertThrows(BusinessException::class.java) { service.login("GOOGLE","token") }
    }

    @Test fun namedRegistrationKeepsLegacyLoginContract() {
        val email = "named-${UUID.randomUUID()}@example.org"
        val id = auth.register(email,"a-test-password",setOf(Role.BUYER,Role.SELLER),"  Scrap collector  ")
        assertEquals(id.toString(),decoder.decode(auth.login(email,"a-test-password")).subject)
        assertEquals("Scrap collector",db.users.sql.sql("select name from account_profiles where user_id=?").param(id).query(String::class.java).single())
    }
}
