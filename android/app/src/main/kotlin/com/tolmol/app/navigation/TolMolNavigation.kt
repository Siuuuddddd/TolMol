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
    DISCOVER("Home"), ACCOUNT("My TolMol"), SEARCH("Search"), ALERTS("Inbox"), LISTINGS("Selling"), BIDS("Bids"), CHAT("Chat"), DEALS("Deals"), WATCHLIST("Watchlist")
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
    val googleSignIn = rememberGoogleSignIn(auth)
    var modeTab by rememberSaveable { mutableStateOf<MarketplaceTab?>(null) }
    val role = state.role
    val account = state.account
    LaunchedEffect(account?.id) {
        if (account != null) {
            register = false
            showForm = false
        }
    }
    BackHandler(enabled = account == null && showForm && !state.loading) {
        auth.clearError()
        showForm = false
    }
    if (account == null && !showForm && !state.loading) {
        WelcomeScreen(
            state = state,
            viewModel = auth,
            signIn = {
                auth.clearError()
                // The primary sign-in action uses the Google account chooser.
                googleSignIn()
            },
            register = {
                auth.clearError()
                register = true
                showForm = true
            },
        )
        return
    }
    if (account == null && showForm) {
        AuthForm(state, register, auth) { auth.clearError(); register = !register }
        return
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        when {
            account == null && state.loading && !showForm -> CircularProgressIndicator(Modifier.padding(24.dp))
            account == null -> AuthForm(state, register, auth) { auth.clearError(); register = !register }
            role == null -> CircularProgressIndicator(Modifier.padding(24.dp))
            else -> key(account.id, role) {
                val otherRole = account.roles.firstOrNull { it != role }
                MarketplaceNavigation(account, role, otherRole?.let { next -> { modeTab=null; auth.select(next) } }, { modeTab=null; auth.logout() }, modeTab) { tab, next -> modeTab=tab; auth.select(next) }
            }
        }
    }
}

@Composable
private fun MarketplaceNavigation(account: Account, role: Role, changeRole: (() -> Unit)?, logout: () -> Unit, initialTab: MarketplaceTab?, changeMode: (MarketplaceTab, Role) -> Unit) {
    val stack = rememberNavBackStack(Home(initialTab ?: if(role == Role.BUYER) MarketplaceTab.DISCOVER else MarketplaceTab.LISTINGS))
    val back = { if (stack.size > 1) stack.removeAt(stack.lastIndex); Unit }
    var location by remember { mutableStateOf<Instant?>(null) }
    val navigate: (MarketplaceTab) -> Unit = { tab ->
        if(tab==MarketplaceTab.LISTINGS && role==Role.BUYER && Role.SELLER in account.roles) changeMode(tab,Role.SELLER)
        else if(tab in listOf(MarketplaceTab.DISCOVER,MarketplaceTab.SEARCH,MarketplaceTab.WATCHLIST) && role==Role.SELLER && Role.BUYER in account.roles) changeMode(tab,Role.BUYER)
        else { stack.clear(); stack.add(Home(tab)) }
    }
    Scaffold(bottomBar = {
        MarketplaceBottomBar((stack.firstOrNull() as? Home)?.tab ?: MarketplaceTab.DISCOVER,navigate)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if(stack.lastOrNull() !is AuctionRoute) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (stack.size > 1) TextButton(back) { Text("Back") }
                else if (changeRole != null) TextButton(changeRole) {
                    Text(if (role == Role.BUYER) "Switch to selling" else "Switch to buying")
                }
                else Text(if (role == Role.BUYER) "Buyer mode" else "Seller mode", Modifier.padding(12.dp))
                TextButton(logout) { Text("Sign out") }
            }
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
                            MarketplaceTab.DISCOVER -> if(role==Role.BUYER) DiscoveryScreen(koinViewModel(), location) { lot ->
                                stack.add(AuctionRoute(lot.id, lot.category, lot.weight.toPlainString(), lot.startingRate.toPlainString(), lot.deadline))
                            } else MyListingsScreen(koinViewModel(), {stack.add(PublishRoute)}) {
                                stack.add(OffersRoute(it.id,it.category,it.status,it.deadline))
                            }
                            MarketplaceTab.SEARCH, MarketplaceTab.WATCHLIST -> {
                                if(role==Role.BUYER) DiscoveryScreen(koinViewModel(),location,
                                    startSearch=route.tab==MarketplaceTab.SEARCH,
                                    watchedOnly=route.tab==MarketplaceTab.WATCHLIST) { lot ->
                                    stack.add(AuctionRoute(lot.id,lot.category,lot.weight.toPlainString(),lot.startingRate.toPlainString(),lot.deadline))
                                }
                                else Screen("Search nearby scrap") {
                                    Text("Switch to buying to search nearby lots.")
                                    if(changeRole!=null) TextButton(changeRole) {Text("Switch to buying")}
                                }
                            }
                            MarketplaceTab.ACCOUNT -> MarketplaceAccountScreen(account.roles.contains(Role.BUYER),navigate,logout)
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
                            { stack.add(DealRoute(lot.id)) }, details = koinViewModel(), recommendations = koinViewModel(),
                            onBack = back, onSearch = {navigate(MarketplaceTab.SEARCH)},
                            onWatchlist = {navigate(MarketplaceTab.WATCHLIST)},
                            onRelatedLot = { next -> stack.add(AuctionRoute(next.id,next.category,next.weight.toPlainString(),next.startingRate.toPlainString(),next.deadline)) })
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
                        LaunchedEffect(vm, route.listing) { vm.open(route.listing) }
                        LaunchedEffect(state.value) { state.value?.let { stack[stack.lastIndex] = ChatRoute(it.id) } }
                        Screen("Opening conversation") { ScreenFeedback(state) { vm.open(route.listing) } }
                    }
                    entry<DealRoute> { DealScreen(koinViewModel(), it.id, account.id) }
                },
            )
        }
    }
}
