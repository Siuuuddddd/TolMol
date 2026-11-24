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
    @Test fun loginShowsAvailableRolesAndLogoutClearsThem() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val account = Account("account-id", setOf(Role.BUYER))
            val repo = object : AuthRepository {
                override val tokenChanges = MutableStateFlow<String?>(null)
                override suspend fun restore(): Account? = null
                override suspend fun login(email: String, password: String): Account {
                    tokenChanges.value = "jwt"
                    return account
                }
                override suspend fun register(email: String, password: String, roles: Set<Role>) = account
                override suspend fun logout() { tokenChanges.value = null }
            }
            val vm = AuthViewModel(AuthUseCase(repo))
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
        } finally { Dispatchers.resetMain() }
    }
}
