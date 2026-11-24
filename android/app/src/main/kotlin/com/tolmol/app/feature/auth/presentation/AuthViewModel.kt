package com.tolmol.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolmol.app.feature.auth.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = true, val account: Account? = null, val role: Role? = null, val error: String? = null)

class AuthViewModel(private val auth: AuthUseCase) : ViewModel() {
    private val mutable = MutableStateFlow(AuthUiState())
    val state = mutable.asStateFlow()

    init {
        restore()
        viewModelScope.launch {
            auth.tokenChanges.drop(1).collect {
                if (it == null) mutable.value = AuthUiState(loading = false)
            }
        }
    }

    fun restore() = submit { auth.restore() }
    fun login(email: String, password: String) = submit { auth.login(email, password) }
    fun register(email: String, password: String, roles: Set<Role>) = submit { auth.register(email, password, roles) }

    fun select(role: Role?) {
        val existing = mutable.value
        if (role == null || role in existing.account?.roles.orEmpty()) mutable.value = existing.copy(role = role)
    }

    fun logout() = submit { auth.logout(); null }

    private fun submit(work: suspend () -> Account?) {
        mutable.value = mutable.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try { mutable.value = AuthUiState(loading = false, account = work()) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { mutable.value = AuthUiState(loading = false, error = error.message ?: "Sign-in failed.") }
        }
    }
}
