package com.tolmol.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.tolmol.app.core.ui.*
import kotlinx.serialization.Serializable
import java.time.Instant
import com.tolmol.app.feature.auth.domain.*
import com.tolmol.app.feature.auth.presentation.*
import com.tolmol.app.feature.location.presentation.*
import com.tolmol.app.feature.discovery.presentation.*
import com.tolmol.app.feature.bidding.presentation.*
import com.tolmol.app.feature.listing.presentation.*

import com.tolmol.app.feature.chat.presentation.*
import com.tolmol.app.feature.notification.presentation.*

@Serializable data class Home(val tab: String) : NavKey
@Serializable data class AuctionRoute(val id: String, val category: String, val weight: String, val opening: String, val deadline: String) : NavKey
@Serializable data object PublishRoute : NavKey
@Serializable data class OffersRoute(val id: String, val category: String, val status: String, val deadline: String) : NavKey
@Serializable data class ConversationsRoute(val listing: String) : NavKey
@Serializable data class ChatRoute(val id: String) : NavKey
@Serializable data class OpenChatRoute(val listing: String) : NavKey
@Serializable data class DealRoute(val id: String) : NavKey

@Composable
fun TolMolNavigation(auth: AuthViewModel = hiltViewModel()) {
    val state by auth.state.collectAsStateWithLifecycle()
    var register by rememberSaveable { mutableStateOf(false) }
    val role = state.role
    val account = state.account
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when {
            account == null -> AuthForm(state, register, auth) { register = !register }
            role == null -> RolePicker(state, auth::select, { auth.logout() })
            else -> key(account.id, role) {
                MarketplaceNavigation(account, role, { auth.select(null) }, { auth.logout() })
            }
        }
    }
}

@Composable
private fun MarketplaceNavigation(account: Account, role: Role, changeRole: () -> Unit, logout: () -> Unit) {
    val tabs = if (role == Role.BUYER) listOf("Discover", "Bids", "Chat", "Alerts") else listOf("Listings", "Chat", "Alerts")
    val stack = rememberNavBackStack(Home(tabs.first()))
    val back = { if (stack.size > 1) stack.removeAt(stack.lastIndex); Unit }
    var location by remember { mutableStateOf<Instant?>(null) }
    Scaffold(bottomBar = {
        if (stack.lastOrNull() is Home && tabs.size > 1) NavigationBar {
            for (tab in tabs) {
                NavigationBarItem(selected = (stack.lastOrNull() as? Home)?.tab == tab,
                    onClick = { stack[0] = Home(tab) },
                    icon = { Text(tab.take(1)) }, label = { Text(tab) })
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (stack.size > 1) TextButton(back) { Text("Back") }
                else TextButton(changeRole) { Text(if (role == Role.BUYER) "Buyer mode" else "Seller mode") }
                TextButton(logout) { Text("Sign out") }
            }
            LocationStatus(role, hiltViewModel()) { location = it }
            NavDisplay(
                backStack = stack,
                modifier = Modifier.weight(1f),
                onBack = back,
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                entryProvider = entryProvider {
                    entry<Home> { route ->
                        when (route.tab) {
                            "Discover" -> DiscoveryScreen(hiltViewModel(), location) { lot ->
                                stack.add(AuctionRoute(lot.id, lot.category, lot.weight.toPlainString(), lot.startingRate.toPlainString(), lot.deadline))
                            }
                            "Bids" -> MyBidsScreen(hiltViewModel()) { Unit }
                            "Listings" -> MyListingsScreen(hiltViewModel(), { stack.add(PublishRoute) }) {
                                stack.add(OffersRoute(it.id, it.category, it.status, it.deadline))
                            }
                            "Chat" -> ConversationsScreen(hiltViewModel(), account.id, null) { stack.add(ChatRoute(it.id)) }
                            "Alerts" -> InboxScreen(hiltViewModel())

                            else -> Screen("TolMol") { Text("Signed in as ${role.name.lowercase()}") }
                        }
                    }
                    entry<AuctionRoute> { lot ->
                        BiddingScreen(hiltViewModel(), lot.id, lot.category, lot.weight, lot.opening, lot.deadline,
                            { stack.add(OpenChatRoute(lot.id)) },
                            null)
                    }
                    entry<PublishRoute> { PublishScreen(hiltViewModel(), back) }
                    entry<OffersRoute> { lot ->
                        OffersScreen(hiltViewModel(), lot.id, lot.category, lot.status, lot.deadline,
                            { stack.add(ConversationsRoute(lot.id)) },
                            null)
                    }
                    entry<ConversationsRoute> { route ->
                        ConversationsScreen(hiltViewModel(), account.id, route.listing) { stack.add(ChatRoute(it.id)) }
                    }
                    entry<ChatRoute> { ChatThreadScreen(hiltViewModel(), it.id, account.id) }
                    entry<OpenChatRoute> { route ->
                        val vm: OpenChatViewModel = hiltViewModel()
                        val state by vm.state.collectAsStateWithLifecycle()
                        LaunchedEffect(route.listing) { vm.open(route.listing) }
                        LaunchedEffect(state.value) { state.value?.let { stack[stack.lastIndex] = ChatRoute(it.id) } }
                        Screen("Opening conversation") { ScreenFeedback(state) { vm.open(route.listing) } }
                    }

                },
            )
        }
    }
}
