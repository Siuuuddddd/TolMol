package com.tolmol.user.service

import com.tolmol.user.domain.model.*
import com.tolmol.user.domain.exception.*
import org.springframework.stereotype.Service
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

interface TokenIssuer { fun issue(id: UUID): String }

@Service
class AuthService(private val store: UserStore, private val encoder: PasswordEncoder, private val issuer: TokenIssuer) {
    private val dummyHash = encoder.encode(UUID.randomUUID().toString())
    fun register(email: String,password: String,roles: Set<Role>): UUID {
        val normalized = email.trim().lowercase()
        require(normalized.length <= 254 && normalized.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) { "Invalid email" }
        require(password.length >= 10 && password.toByteArray().size <= 72 && roles.isNotEmpty()) { "Invalid password or roles" }
        val id = UUID.randomUUID()
        store.register(Account(id,normalized,encoder.encode(password),roles))
        return id
    }
    fun login(email: String,password: String): String {
        val account = store.find(email.trim().lowercase())
        if (!encoder.matches(password,account?.passwordHash ?: dummyHash) || account == null) throw InvalidCredentialsException()
        return issuer.issue(account.id)
    }
    fun roles(id: UUID): Set<Role> = Role.entries.filter { store.hasRole(id, it) }.toSet()
    fun requireRole(id: UUID,role: Role) { if (!store.hasRole(id,role)) throw RoleRequiredException() }
    fun locate(id: UUID,role: Role,position: Position) {
        requireRole(id,role)
        store.savePosition(id,role,position)
    }
    fun position(id: UUID,role: Role): Position {
        requireRole(id,role)
        return store.recentPosition(id,role) ?: throw LocationRequiredException()
    }
}
