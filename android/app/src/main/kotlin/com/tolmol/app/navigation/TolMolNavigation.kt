package com.tolmol.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
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
import com.tolmol.app.feature.deal.presentation.*

@Serializable
enum class MarketplaceTab(val title: String) {
    DISCOVER("Discover"), LISTINGS("Listings"), BIDS("Bids"), CHAT("Chat"), ALERTS("Alerts"), DEALS("Deals")
}

@Serializable data class Home(val tab: MarketplaceTab) : NavKey
@Serializable data class AuctionRoute(val id: String, val category: String, val weight: String, val opening: String, val deadline: String) : NavKey
@Serializable data object PublishRoute : NavKey
@Serializable data class OffersRoute(val id: String, val category: String, val status: String, val deadline: String) : NavKey
@Serializable data class ConversationsRoute(val listing: String) : NavKey
@Serializable data class ChatRoute(val id: String) : NavKey
@Serializable data class OpenChatRoute(val listing: String) : NavKey
@Serializable data class DealRoute(val id: String) : NavKey

@Composable
fun TolMolNavigation(auth: AuthViewModel = koinViewModel()) {
    val state by auth.state.collectAsStateWithLifecycle()
    var register by rememberSaveable { mutableStateOf(false) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var preferredRole by rememberSaveable { mutableStateOf<Role?>(null) }
    val role = state.role
    val account = state.account
    LaunchedEffect(account?.id) {
        if (account != null) {
            register = false
            showForm = true
        }
    }
    BackHandler(enabled = account == null && showForm && !state.loading && state.error == null) {
        showForm = false
    }
    if (account == null && !showForm && !state.loading && state.error == null) {
        WelcomeScreen(
            signIn = {
                preferredRole = null
                register = false
                showForm = true
            },
            register = { selectedRole ->
                preferredRole = selectedRole
                register = true
                showForm = true
            },
        )
        return
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        when {
            account == null && state.loading && !showForm -> CircularProgressIndicator(Modifier.padding(24.dp))
            account == null -> AuthForm(state, register, preferredRole, auth) { register = !register }
            role == null -> RolePicker(state, auth::select, { auth.logout() })
            else -> key(account.id, role) {
                MarketplaceNavigation(account, role, { auth.select(null) }, { auth.logout() })
            }
        }
    }
}

@Composable
private fun MarketplaceNavigation(account: Account, role: Role, changeRole: () -> Unit, logout: () -> Unit) {
    val sharedTabs = listOf(MarketplaceTab.CHAT, MarketplaceTab.ALERTS, MarketplaceTab.DEALS)
    val tabs = (if (role == Role.BUYER) listOf(MarketplaceTab.DISCOVER, MarketplaceTab.BIDS)
        else listOf(MarketplaceTab.LISTINGS)) + sharedTabs
    val stack = rememberNavBackStack(Home(tabs.first()))
    val back = { if (stack.size > 1) stack.removeAt(stack.lastIndex); Unit }
    var location by remember { mutableStateOf<Instant?>(null) }
    Scaffold(bottomBar = {
        if (stack.lastOrNull() is Home && tabs.size > 1) NavigationBar {
            for (tab in tabs) {
                NavigationBarItem(selected = (stack.lastOrNull() as? Home)?.tab == tab,
                    onClick = { stack[0] = Home(tab) },
                    icon = { Text(tab.title.take(1)) }, label = { Text(tab.title) })
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (stack.size > 1) TextButton(back) { Text("Back") }
                else TextButton(changeRole) { Text(if (role == Role.BUYER) "Buyer mode" else "Seller mode") }
                TextButton(logout) { Text("Sign out") }
            }
            LocationStatus(role, koinViewModel()) { location = it }
            NavDisplay(
                backStack = stack,
                modifier = Modifier.weight(1f),
                onBack = back,
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                entryProvider = entryProvider {
                    entry<Home> { route ->
                        when (route.tab) {
                            MarketplaceTab.DISCOVER -> DiscoveryScreen(koinViewModel(), location) { lot ->
                                stack.add(AuctionRoute(lot.id, lot.category, lot.weight.toPlainString(), lot.startingRate.toPlainString(), lot.deadline))
                            }
                            MarketplaceTab.BIDS -> MyBidsScreen(koinViewModel()) { stack.add(DealRoute(it)) }
                            MarketplaceTab.LISTINGS -> MyListingsScreen(koinViewModel(), { stack.add(PublishRoute) }) {
                                stack.add(OffersRoute(it.id, it.category, it.status, it.deadline))
                            }
                            MarketplaceTab.CHAT -> ConversationsScreen(koinViewModel(), account.id, null) { stack.add(ChatRoute(it.id)) }
                            MarketplaceTab.ALERTS -> InboxScreen(koinViewModel())
                            MarketplaceTab.DEALS -> DealsScreen(koinViewModel(), account.id, role == Role.SELLER) { stack.add(DealRoute(it)) }
                        }
                    }
                    entry<AuctionRoute> { lot ->
                        BiddingScreen(koinViewModel(), lot.id, lot.category, lot.weight, lot.opening, lot.deadline,
                            { stack.add(OpenChatRoute(lot.id)) },
                            { stack.add(DealRoute(lot.id)) })
                    }
                    entry<PublishRoute> { PublishScreen(koinViewModel(), back) }
                    entry<OffersRoute> { lot ->
                        OffersScreen(koinViewModel(), lot.id, lot.category, lot.status, lot.deadline,
                            { stack.add(ConversationsRoute(lot.id)) },
                            { stack.add(DealRoute(lot.id)) })
                    }
                    entry<ConversationsRoute> { route ->
                        ConversationsScreen(koinViewModel(), account.id, route.listing) { stack.add(ChatRoute(it.id)) }
                    }
                    entry<ChatRoute> { ChatThreadScreen(koinViewModel(), it.id, account.id) }
                    entry<OpenChatRoute> { route ->
                        val vm: OpenChatViewModel = koinViewModel()
                        val state by vm.state.collectAsStateWithLifecycle()
                        LaunchedEffect(route.listing) { vm.open(route.listing) }
                        LaunchedEffect(state.value) { state.value?.let { stack[stack.lastIndex] = ChatRoute(it.id) } }
                        Screen("Opening conversation") { ScreenFeedback(state) { vm.open(route.listing) } }
                    }
                    entry<DealRoute> { DealScreen(koinViewModel(), it.id, account.id) }
                },
            )
        }
    }
}
