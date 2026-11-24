package com.tolmol.app.feature.auth.data

import com.tolmol.app.core.datastore.SessionStore
import com.tolmol.app.core.datastore.expiry
import com.tolmol.app.core.network.serverMessage
import com.tolmol.app.feature.auth.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.time.Instant

class AuthRepositoryImpl(private val api: AuthApi, private val session: SessionStore) : AuthRepository {
    override val tokenChanges: Flow<String?> = session.current

    override suspend fun restore(): Account? {
        val token = session.read() ?: return null
        if (expiry(token)?.isAfter(Instant.now()) != true) {
            session.clear(token)
            return null
        }
        return try { account(token) }
        catch (error: HttpException) {
            if (error.code() != 401) throw error
            session.clear(token)
            null
        }
    }

    override suspend fun login(email: String, password: String): Account = authRequest {
        val token = api.login(CredentialsDto(email.trim(), password)).accessToken
        val profile = account(token)
        session.save(token)
        profile
    }

    override suspend fun register(email: String, password: String, roles: Set<Role>): Account = authRequest {
        api.register(RegisterDto(email.trim(), password, roles.map { it.name }.toSet()))
        login(email, password)
    }

    override suspend fun logout() = session.clear()

    private suspend fun account(token: String): Account = api.profile("Bearer $token").let { dto ->
        Account(dto.id, dto.roles.mapNotNull { name -> Role.entries.find { it.name == name } }.toSet())
    }

    private suspend fun <T> authRequest(block: suspend () -> T): T = try { block() }
    catch (error: CancellationException) { throw error }
    catch (error: AuthError) { throw error }
    catch (error: Exception) {
        if (error is HttpException && error.code() == 401) throw AuthError.InvalidCredentials
        throw AuthError.RequestFailed(error.serverMessage())
    }
}
