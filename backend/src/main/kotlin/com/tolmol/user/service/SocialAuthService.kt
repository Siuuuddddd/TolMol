package com.tolmol.user.service

import com.tolmol.platform.domain.exception.BusinessException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

data class SocialIdentity(val provider: String, val subject: String, val email: String, val name: String)
interface SocialIdentityVerifier { fun verify(provider: String, token: String): SocialIdentity }

@Service
class SocialAuthService(
    private val verifier: SocialIdentityVerifier,
    private val users: UserStore,
    private val encoder: PasswordEncoder,
    private val issuer: TokenIssuer,
) {
    fun login(provider: String, token: String): String {
        require(token.isNotBlank() && token.length <= 16384) { "Invalid provider token" }
        val identity = verifier.verify(provider,token)
        if (identity.subject.isBlank() || identity.email.length > 254 ||
            !identity.email.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) {
            throw BusinessException(401,"The provider did not supply a verified email.")
        }
        val user = users.socialAccount(identity,encoder.encode(UUID.randomUUID().toString()))
        return issuer.issue(user)
    }
}
