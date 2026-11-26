package com.tolmol.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tolmol.app.R
import com.tolmol.app.core.ui.*

@Composable
fun MarketplaceBottomBar(current: MarketplaceTab, select: (MarketplaceTab) -> Unit) {
    val selected = when(current) {
        MarketplaceTab.BIDS, MarketplaceTab.CHAT, MarketplaceTab.DEALS, MarketplaceTab.WATCHLIST -> MarketplaceTab.ACCOUNT
        else -> current
    }
    val items=listOf(MarketplaceTab.DISCOVER to R.drawable.ic_ui_home,
        MarketplaceTab.ACCOUNT to R.drawable.ic_ui_account, MarketplaceTab.SEARCH to R.drawable.ic_ui_search,
        MarketplaceTab.ALERTS to R.drawable.ic_ui_bell, MarketplaceTab.LISTINGS to R.drawable.ic_ui_tag)
    NavigationBar(Modifier.height(64.dp),containerColor=MaterialTheme.colorScheme.surface,
        windowInsets=WindowInsets(0,0,0,0)) {
        items.forEach { (tab,icon) ->
            NavigationBarItem(selected=selected==tab,onClick={select(tab)},
                icon={NativeIcon(icon,null)},label={Text(tab.title,fontSize=12.sp,maxLines=1)},
                colors=NavigationBarItemDefaults.colors(selectedIconColor=MaterialTheme.colorScheme.primary,
                    selectedTextColor=MaterialTheme.colorScheme.primary,
                    indicatorColor=MaterialTheme.colorScheme.primary.copy(alpha=0.12f),
                    unselectedIconColor=MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor=MaterialTheme.colorScheme.onSurface))
        }
    }
}

@Composable
fun MarketplaceAccountScreen(buyer: Boolean, open: (MarketplaceTab)->Unit, logout: ()->Unit) {
    Screen("My TolMol") {
        Text("Your activity",style=MaterialTheme.typography.titleMedium)
        if(buyer) {
            RowCard("My bids","Follow your offers and auction results") {open(MarketplaceTab.BIDS)}
            RowCard("Watchlist","Your saved nearby scrap lots") {open(MarketplaceTab.WATCHLIST)}
        }
        RowCard("Messages","Conversations with buyers and sellers") {open(MarketplaceTab.CHAT)}
        RowCard("My deals","Pickup and settlement") {open(MarketplaceTab.DEALS)}
        RowCard("Notifications","Updates about your activity") {open(MarketplaceTab.ALERTS)}
        TextButton(logout) {Text("Sign out")}
    }
}
