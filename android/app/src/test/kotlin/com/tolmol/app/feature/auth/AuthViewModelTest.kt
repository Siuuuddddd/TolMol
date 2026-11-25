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
    @Test fun signInRegistrationAndRestoreOpenAllowedHomeWithoutRolePrompt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var account = Account("account-id", setOf(Role.BUYER))
            var restoredAccount: Account? = null
            var receivedIdToken: String? = null
            var googleFailure = false
            val repo = object : AuthRepository {
                override val tokenChanges = MutableStateFlow<String?>(null)
                override suspend fun restore(): Account? = restoredAccount
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
            assertEquals(Role.BUYER, vm.state.value.role)
            vm.select(Role.SELLER)
            assertEquals(Role.BUYER, vm.state.value.role)
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
            account = account.copy(roles = setOf(Role.BUYER,Role.SELLER))
            vm.google("provider-id-token")
            runCurrent()
            assertEquals("provider-id-token",receivedIdToken)
            assertEquals(account,vm.state.value.account)
            assertNull(vm.state.value.error)
            assertEquals(Role.BUYER,vm.state.value.role)
            vm.select(Role.SELLER)
            assertEquals(Role.SELLER,vm.state.value.role)
            vm.registerProfile("Recycler","recycler@example.test","valid password")
            runCurrent()
            assertEquals(Role.BUYER,vm.state.value.role)
            restoredAccount = account.copy(roles = setOf(Role.SELLER))
            vm.restore()
            runCurrent()
            assertEquals(Role.SELLER,vm.state.value.role)
            assertEquals(restoredAccount,vm.state.value.account)
        } finally { Dispatchers.resetMain() }
    }
}
