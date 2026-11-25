package com.tolmol.app.feature.auth.domain

import kotlinx.coroutines.flow.Flow

enum class Role { BUYER, SELLER }
data class Account(val id: String, val roles: Set<Role>)

sealed class AuthError(message: String) : RuntimeException(message) {
    data object InvalidCredentials : AuthError("Check your email and password.")
    class RequestFailed(message: String) : AuthError(message)
}

interface AuthRepository {
    val tokenChanges: Flow<String?>
    suspend fun restore(): Account?
    suspend fun login(email: String, password: String): Account
    suspend fun register(email: String, password: String, roles: Set<Role>): Account
    suspend fun registerProfile(name: String, email: String, password: String): Account
    suspend fun signInWithGoogle(idToken: String): Account
    suspend fun logout()
}

class AuthUseCase(repository: AuthRepository) : AuthRepository by repository

class GoogleSignInUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Account = repository.signInWithGoogle(idToken)
}
