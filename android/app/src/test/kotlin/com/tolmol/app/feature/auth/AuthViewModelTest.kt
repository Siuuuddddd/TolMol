package com.tolmol.app.feature.auth

import com.tolmol.app.feature.auth.domain.*
import com.tolmol.app.feature.auth.presentation.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @Test fun passwordAndGoogleSessionsKeepRolesAndHandleRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val account = Account("account-id", setOf(Role.BUYER))
            var receivedIdToken: String? = null
            var googleFailure = false
            val repo = object : AuthRepository {
                override val tokenChanges = MutableStateFlow<String?>(null)
                override suspend fun restore(): Account? = null
                override suspend fun login(email: String, password: String): Account {
                    tokenChanges.value = "jwt"
                    return account
                }
                override suspend fun register(email: String, password: String, roles: Set<Role>) = account
                override suspend fun registerProfile(name: String, email: String, password: String) = account
                override suspend fun signInWithGoogle(idToken: String): Account {
                    receivedIdToken = idToken
                    if (googleFailure) throw AuthError.RequestFailed("Check your connection and retry.")
                    tokenChanges.value = "google-session-jwt"
                    return account
                }
                override suspend fun logout() { tokenChanges.value = null }
            }
            val vm = AuthViewModel(AuthUseCase(repo),GoogleSignInUseCase(repo))
            runCurrent()
            vm.login("buyer@example.test", "valid password")
            runCurrent()
            assertEquals(account, vm.state.value.account)
            vm.select(Role.SELLER)
            assertNull(vm.state.value.role)
            vm.select(Role.BUYER)
            assertEquals(Role.BUYER, vm.state.value.role)
            vm.logout()
            runCurrent()
            assertNull(vm.state.value.account)
            googleFailure = true
            vm.google("provider-id-token")
            runCurrent()
            assertFalse(vm.state.value.loading)
            assertNotNull(vm.state.value.error)
            assertNull(vm.state.value.account)
            googleFailure = false
            vm.google("provider-id-token")
            runCurrent()
            assertEquals("provider-id-token",receivedIdToken)
            assertEquals(account,vm.state.value.account)
            assertNull(vm.state.value.error)
            assertNull(vm.state.value.role)
        } finally { Dispatchers.resetMain() }
    }
}
