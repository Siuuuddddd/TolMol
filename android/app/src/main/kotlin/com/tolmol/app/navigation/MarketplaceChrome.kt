package com.tolmol.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tolmol.app.core.ui.components.BrickBottomBar
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
    BrickBottomBar(items.indexOfFirst {it.first==selected},items.map {it.first.title},items.map {it.second}) { index ->
        select(items[index].first)
    }
}
