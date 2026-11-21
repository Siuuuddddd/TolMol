package com.tolmol
import com.tolmol.user.service.AuthService
import com.tolmol.user.domain.model.*
import com.tolmol.user.domain.exception.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.jwt.JwtDecoder

class UserTest : BackendTest() {
    @Autowired lateinit var auth: AuthService
    @Autowired lateinit var decoder: JwtDecoder
    @Test fun identityRolesAndLocation() {
        val id = auth.register("Buyer@example.org","a-test-password",setOf(Role.BUYER))
        assertEquals(id.toString(),decoder.decode(auth.login("buyer@example.org","a-test-password")).subject)
        assertThrows(InvalidCredentialsException::class.java) { auth.login("buyer@example.org","incorrect") }
        assertThrows(EmailTakenException::class.java) { auth.register("buyer@example.org","a-test-password",setOf(Role.SELLER)) }
        val p = Position(19.07,72.87)
        auth.locate(id,Role.BUYER,p)
        assertEquals(p,auth.position(id,Role.BUYER))
        assertThrows(RoleRequiredException::class.java) { auth.locate(id,Role.SELLER,p) }
    }
}
