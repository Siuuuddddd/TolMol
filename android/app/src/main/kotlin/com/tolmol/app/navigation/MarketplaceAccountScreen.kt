package com.tolmol.app.navigation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tolmol.app.R
import com.tolmol.app.core.common.LiveUpdate
import com.tolmol.app.core.ui.NativeIcon
import com.tolmol.app.core.ui.components.*
import com.tolmol.app.core.ui.theme.*
import com.tolmol.app.feature.auth.domain.*
import com.tolmol.app.feature.discovery.domain.DiscoveryUseCase
import com.tolmol.app.feature.notification.domain.NotificationUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.compose.koinInject

internal data class AccountOverviewState(val loading:Boolean=true,val watched:Int?=null,
    val unread:Boolean=false,val error:String?=null)

internal class AccountOverviewViewModel(private val discovery:DiscoveryUseCase,
    private val notifications:NotificationUseCase):ViewModel() {
    private val mutable=MutableStateFlow(AccountOverviewState())
    val state=mutable.asStateFlow()
    private var job:Job?=null
    fun refresh(canBuy:Boolean) {
        if(job?.isActive==true) return
        job=viewModelScope.launch {
            mutable.value=mutable.value.copy(loading=true,error=null)
            try {
                coroutineScope {
                    val watched=async {if(canBuy) discovery.watched().size else null}
                    val alerts=async {notifications.inbox().any {!it.read}}
                    mutable.value=AccountOverviewState(loading=false,watched=watched.await(),unread=alerts.await())
                }
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {mutable.value=mutable.value.copy(loading=false,error="Could not load your updates. Please retry.")}
        }
    }
}

@Composable
fun MarketplaceAccountScreen(account:Account?,role:Role,open:(MarketplaceTab)->Unit,logout:()->Unit,
    publish:()->Unit,changeRole:(()->Unit)?) {
    val discovery:DiscoveryUseCase=koinInject()
    val notifications:NotificationUseCase=koinInject()
    val factory=remember(discovery,notifications) {viewModelFactory {initializer {AccountOverviewViewModel(discovery,notifications)}}}
    val vm:AccountOverviewViewModel=viewModel(key="my-tolmol-${account?.id}",factory=factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val canBuy=account?.roles?.contains(Role.BUYER)==true
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {if(account!=null) vm.refresh(canBuy)}
    LaunchedEffect(vm,account?.id,canBuy) {
        if(account!=null) notifications.live.collect {
            if(it is LiveUpdate.Event || it is LiveUpdate.Connected) vm.refresh(canBuy)
        }
    }
    MyTolMolContent(account!=null,role,account?.roles.orEmpty(),state,open,logout,publish,changeRole,{vm.refresh(canBuy)})
}

@Composable
internal fun MyTolMolContent(signedIn:Boolean,role:Role,roles:Set<Role>,state:AccountOverviewState,
    open:(MarketplaceTab)->Unit,logout:()->Unit,publish:()->Unit,changeRole:(()->Unit)?,retry:()->Unit) {
    Column(Modifier.fillMaxSize().background(GameColors.Cream)) {
        Row(Modifier.fillMaxWidth().heightIn(min=GameDimens.Toolbar).padding(horizontal=GameDimens.Margin),
            verticalAlignment=Alignment.CenterVertically) {
            OutlinedPixelText("My TolMol",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall)
            AccountBadge(R.drawable.ic_account_mail,"Messages",false) {open(MarketplaceTab.CHAT)}
            AccountBadge(R.drawable.ic_ui_bell,"Notifications",state.unread) {open(MarketplaceTab.ALERTS)}
            if(changeRole!=null) {
                var menu by remember {mutableStateOf(false)}
                Box {
                    IconButton({menu=true}) {NativeIcon(R.drawable.ic_ui_more,"Account options")}
                    DropdownMenu(menu,{menu=false}) {
                        DropdownMenuItem(text={Text(if(role==Role.BUYER) "Switch to selling" else "Switch to buying")},
                            onClick={menu=false;changeRole()})
                    }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=GameDimens.Margin),
            verticalArrangement=Arrangement.spacedBy(GameDimens.SmallGap)) {
            if(!signedIn) {
                EmptyState("You are signed out","Sign in to see your account activity.")
            } else {
                if(state.loading && state.watched==null) SkeletonBrick(Modifier.fillMaxWidth().height(88.dp))
                else ProfileHeader("TolMol member")
                SectionTitle("Your updates")
                if(state.error!=null) BrickCard(Modifier.fillMaxWidth()) {
                    Text(state.error,style=MaterialTheme.typography.bodySmall,color=GameColors.RedShadow)
                    BrickSecondaryButton(retry) {Text("Retry")}
                }
                if(state.loading && state.watched==null) Row(horizontalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
                    repeat(2) {SkeletonBrick(Modifier.weight(1f).height(176.dp))}
                } else if(Role.BUYER in roles) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
                    UpdateCard("Watchlist","Your saved nearby scrap lots",state.watched,Modifier.weight(1f).fillMaxHeight()) {open(MarketplaceTab.WATCHLIST)}
                    UpdateCard("Bids & offers","Active auctions and seller offers",null,Modifier.weight(1f).fillMaxHeight()) {open(MarketplaceTab.BIDS)}
                } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
                    UpdateCard("My listings","Your active lots and offers",null,Modifier.weight(1f).fillMaxHeight()) {open(MarketplaceTab.LISTINGS)}
                    UpdateCard("My deals","Pickup and settlement",null,Modifier.weight(1f).fillMaxHeight()) {open(MarketplaceTab.DEALS)}
                }
                if(role==Role.BUYER) {
                    SectionTitle("Buying")
                    IconRow(R.drawable.ic_ui_heart,"Watchlist","Keep tabs on watched lots") {open(MarketplaceTab.WATCHLIST)}
                    IconRow(R.drawable.ic_account_gavel,"Bids & offers","Active auctions and your offers") {open(MarketplaceTab.BIDS)}
                    IconRow(R.drawable.ic_account_package,"My deals","Pickup and settlement") {open(MarketplaceTab.DEALS)}
                    IconRow(R.drawable.ic_ui_chat,"Messages","Conversations with sellers") {open(MarketplaceTab.CHAT)}
                }
                if(Role.SELLER in roles) {
                    SectionTitle("Selling")
                    IconRow(R.drawable.ic_account_add,"List scrap","Publish a new lot",publish)
                    IconRow(R.drawable.ic_ui_tag,"My listings","Your active lots and offers") {open(MarketplaceTab.LISTINGS)}
                    IconRow(R.drawable.ic_account_gavel,"Offers received","Review and accept bids") {open(MarketplaceTab.LISTINGS)}
                    IconRow(R.drawable.ic_ui_chat,"Messages","Conversations with buyers") {open(MarketplaceTab.CHAT)}
                    IconRow(R.drawable.ic_account_package,"My deals","Pickup and settlement") {open(MarketplaceTab.DEALS)}
                }
                SectionTitle("Shortcuts")
                IconRow(R.drawable.ic_ui_bell,"Notifications","Updates about your activity") {open(MarketplaceTab.ALERTS)}
                SectionTitle("Account")
                AccountRow("Sign out",logout,destructive=true)
            }
            HorizontalDivider(Modifier.padding(vertical=GameDimens.Gap),color=GameColors.BorderLight)
            BrickWallFooter(Modifier.fillMaxWidth())
            Spacer(Modifier.height(GameDimens.SectionGap))
        }
    }
}

@Composable
private fun AccountBadge(icon:Int,label:String,unread:Boolean,onClick:()->Unit) {
    Box(Modifier.size(48.dp),contentAlignment=Alignment.Center) {
        IconButton(onClick,Modifier.size(40.dp).clip(CircleShape).background(GameColors.Paper)
            .border(GameDimens.Border,GameColors.Navy,CircleShape)) {NativeIcon(icon,label)}
        if(unread) Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(9.dp).clip(CircleShape).background(GameColors.Red)
            .border(1.dp,GameColors.Cream,CircleShape))
    }
}

@Preview(name="My TolMol buyer",widthDp=390,heightDp=844)
@Composable
fun MyTolMolBuyerPreview() {TolMolTheme {MyTolMolContent(true,Role.BUYER,Role.entries.toSet(),
    AccountOverviewState(false,4,true),{},{},{},{},{})}}

@Preview(name="My TolMol seller",widthDp=390,heightDp=844)
@Composable
fun MyTolMolSellerPreview() {TolMolTheme {MyTolMolContent(true,Role.SELLER,setOf(Role.SELLER),
    AccountOverviewState(false),{},{},{},{},{})}}
