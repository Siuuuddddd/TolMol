package com.tolmol.user.service

import org.springframework.stereotype.Service

data class GoogleIdentity(val subject: String, val email: String, val name: String)
fun interface GoogleIdentityVerifier { fun verify(idToken: String): GoogleIdentity }

@Service
class GoogleAuthService(
    private val verifier: GoogleIdentityVerifier,
    private val users: UserStore,
    private val issuer: TokenIssuer,
) {
    fun login(idToken: String): String {
        require(idToken.isNotBlank() && idToken.length <= 16384) { "Invalid Google token" }
        val identity = verifier.verify(idToken)
        return issuer.issue(users.googleAccount(identity))
    }
}
