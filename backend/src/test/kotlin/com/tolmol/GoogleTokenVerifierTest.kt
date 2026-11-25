package com.tolmol

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.*
import com.tolmol.platform.domain.exception.BusinessException
import com.tolmol.user.infra.security.GoogleTokenVerifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.mock.env.MockEnvironment
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.time.Instant
import java.util.Date

class GoogleTokenVerifierTest {
    private val key = RSAKeyGenerator(2048).generate()
    private val env = MockEnvironment().withProperty("GOOGLE_CLIENT_ID","test-web-client")
    private val verifier = GoogleTokenVerifier(env,NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build())
    private fun token(audience: String = "test-web-client", verified: Boolean = true,
                      issuer: String = "https://accounts.google.com", expires: Instant = Instant.now().plusSeconds(300)): String {
        val claims = JWTClaimsSet.Builder().issuer(issuer).subject("google-subject").audience(audience)
            .issueTime(Date.from(Instant.now().minusSeconds(5))).expirationTime(Date.from(expires))
            .claim("email","recycler@gmail.com").claim("email_verified",verified).claim("name","Recycler").build()
        return SignedJWT(JWSHeader(JWSAlgorithm.RS256),claims).apply { sign(RSASSASigner(key)) }.serialize()
    }

    @Test fun validSignatureAndClaimsAreAccepted() {
        assertEquals("google-subject",verifier.verify(token()).subject)
        assertEquals("recycler@gmail.com",verifier.verify(token(issuer="accounts.google.com")).email)
    }
    @Test fun wrongAudienceIsRejected() {
        assertThrows(BusinessException::class.java) { verifier.verify(token(audience="another-app")) }
    }
    @Test fun invalidSignatureAndMalformedTokenAreRejected() {
        val other = RSAKeyGenerator(2048).generate()
        val jwt = SignedJWT(JWSHeader(JWSAlgorithm.RS256),SignedJWT.parse(token()).jwtClaimsSet).apply { sign(RSASSASigner(other)) }.serialize()
        assertThrows(BusinessException::class.java) { verifier.verify(jwt) }
        assertThrows(BusinessException::class.java) { verifier.verify("not-a-token") }
    }
    @Test fun expiredUnverifiedAndForeignIssuerTokensAreRejected() {
        assertThrows(BusinessException::class.java) { verifier.verify(token(expires=Instant.now().minusSeconds(300))) }
        assertThrows(BusinessException::class.java) { verifier.verify(token(verified=false)) }
        assertThrows(BusinessException::class.java) { verifier.verify(token(issuer="https://attacker.invalid")) }
    }
    @Test fun absentConfigurationFailsClosed() {
        val missing = GoogleTokenVerifier(MockEnvironment(),NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build())
        assertThrows(BusinessException::class.java) { missing.verify(token()) }
    }
}
