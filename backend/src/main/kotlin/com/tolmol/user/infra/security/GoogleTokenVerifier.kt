package com.tolmol.user.infra.security

import com.tolmol.platform.domain.exception.BusinessException
import com.tolmol.user.service.GoogleIdentity
import com.tolmol.user.service.GoogleIdentityVerifier
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GoogleTokenVerifier @Autowired constructor(private val env: Environment) : GoogleIdentityVerifier {
    private var testDecoder: JwtDecoder? = null
    internal constructor(env: Environment, decoder: JwtDecoder) : this(env) { testDecoder = decoder }
    private val decoder by lazy {
        testDecoder ?: NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build()
    }

    override fun verify(idToken: String): GoogleIdentity {
        val clientId = env.getProperty("GOOGLE_CLIENT_ID")?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw BusinessException(503,"Google sign-in is not configured yet")
        val claims = try { decoder.decode(idToken) }
            catch (_: JwtException) { throw BusinessException(401,"Google could not verify this sign-in. Please try again.") }
        val subject = claims.subject.orEmpty()
        val email = claims.getClaimAsString("email").orEmpty().trim().lowercase()
        if (claims.getClaimAsString("iss") !in setOf("accounts.google.com","https://accounts.google.com") ||
            clientId !in claims.audience || claims.expiresAt?.isAfter(Instant.now()) != true ||
            claims.getClaimAsBoolean("email_verified") != true || subject.length !in 1..255 ||
            email.length > 254 || !email.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) {
            throw BusinessException(401,"Google could not verify this sign-in. Please try again.")
        }
        return GoogleIdentity(subject,email,claims.getClaimAsString("name").orEmpty().trim().take(100).ifBlank { "TolMol user" })
    }
}
