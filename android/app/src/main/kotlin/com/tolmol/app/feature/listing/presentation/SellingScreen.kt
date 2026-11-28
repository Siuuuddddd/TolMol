package com.tolmol.app.feature.listing.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolmol.app.R
import com.tolmol.app.core.ui.*
import com.tolmol.app.core.ui.components.*
import com.tolmol.app.core.ui.theme.*
import com.tolmol.app.feature.listing.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

@Composable
fun MyListingsScreen(vm: ListingViewModel, publish: () -> Unit, deals: () -> Unit, open: (Listing) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.refresh() }
    SellingContent(state, vm::refresh, publish, deals, open)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellingContent(
    state: ScreenState<SellingData>, refresh: () -> Unit, publish: () -> Unit,
    deals: () -> Unit, open: (Listing) -> Unit,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var listingsY by remember { mutableIntStateOf(0) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = Instant.now() } }
    val data = state.value
    val active = data?.listings.orEmpty().filter { it.status == "OPEN" && Instant.parse(it.deadline).isAfter(now) }
    PullToRefreshBox(state.loading, refresh, Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(GameDimens.Margin),
            verticalArrangement = Arrangement.spacedBy(GameDimens.Gap)) {
            BrickButton(publish, Modifier.fillMaxWidth()) { Text("Publish a listing") }
            if (state.loading && data == null) {
                SkeletonBrick(Modifier.fillMaxWidth())
            } else {
                Row(Modifier.fillMaxWidth()) {
                    StatBlock("ACTIVE", active.size, Modifier.weight(1f))
                    StatBlock("ORDERS", data?.orders ?: 0, Modifier.weight(1f))
                }
            }
            if (state.error != null) {
                BrickCard(Modifier.fillMaxWidth()) {
                    Text(state.error, color = GameColors.Navy)
                    BrickSecondaryButton(refresh) { Text("Retry") }
                }
            }
            SectionTitle("Selling tips")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(GameDimens.Gap)) {
                item { TipCard("Add clear details", "Check your title, weight and opening rate.", "1", "List") }
                item { TipCard("Review your offers", "Compare rates before accepting a bid.", "2", "Review") }
                item { TipCard("Plan your pickup", "Confirm the weight with your buyer.", "3", "Pickup") }
            }
            SectionTitle("Selling details")
            BrickCard(Modifier.fillMaxWidth()) {
                DetailRow("Offers received", "Bids waiting for your review",
                    data?.let { active.sumOf { lot -> it.offers[lot.id].orEmpty().size }.toString() }) {
                    scope.launch { scroll.animateScrollTo(listingsY) }
                }
                HorizontalDivider(color = GameColors.Muted)
                DetailRow("Deals", "Pickup and settlement", data?.orders?.toString(), deals)
            }
            Box(Modifier.onGloballyPositioned { listingsY = it.positionInParent().y.toInt() }) { SectionTitle("My listings") }
            if (state.loading && data == null) {
                repeat(2) { SkeletonBrick(Modifier.fillMaxWidth().height(140.dp)) }
            } else if (data != null && data.listings.isEmpty()) {
                EmptyState("No listings yet", "Your published scrap lots appear here.")
                YellowUnderlineLink("Publish a listing", publish, Modifier.heightIn(min = 48.dp))
            } else {
                data?.listings.orEmpty().sortedBy { Instant.parse(it.deadline) }.forEach { lot ->
                    ListingRowCard(lot, data?.offers?.get(lot.id), now) { open(lot) }
                }
            }
            SectionTitle("How it works")
            HowItWorksRow(R.drawable.ic_account_add, "List in minutes",
                "Add a title, category, weight and your opening rate per kg.")
            HowItWorksRow(R.drawable.ic_ui_tag, "Get offers nearby",
                "Buyers within 20 km can see your lot and place bids.")
            HowItWorksRow(R.drawable.ic_account_package, "Choose and complete",
                "Accept any valid bid, chat, then confirm weight at pickup.")
            HorizontalDivider(color = GameColors.Muted)
            BrickWallFooter(Modifier.fillMaxWidth())
            Spacer(Modifier.height(GameDimens.Gap))
        }
    }
}

@Composable
private fun StatBlock(label: String, count: Int, modifier: Modifier) {
    Column(modifier.padding(vertical = GameDimens.Gap)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
        Text(count.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TipCard(title: String, hint: String, number: String, caption: String) {
    BrickCard(Modifier.width(300.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GameDimens.Gap)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
            }
            BadgeRing(number, caption)
        }
    }
}

@Composable
private fun BadgeRing(number: String, caption: String) {
    Column(Modifier.size(72.dp).background(GameColors.Yellow, CircleShape)
        .border(GameDimens.Border, GameColors.Navy, CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(caption, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DetailRow(title: String, subtitle: String, count: String?, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = action),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
        }
        if (count != null) Text(count, fontWeight = FontWeight.Bold)
        NativeIcon(R.drawable.ic_ui_chevron, null)
    }
}

@Composable
private fun ListingRowCard(lot: Listing, offers: List<Offer>?, now: Instant, open: () -> Unit) {
    val seconds = Duration.between(now, Instant.parse(lot.deadline)).seconds
    BrickCard(Modifier.fillMaxWidth(), onClick = open) {
        Row(horizontalArrangement = Arrangement.spacedBy(GameDimens.Gap)) {
            ScrapImage(lot.imageKey, lot.category, Modifier.size(88.dp), lot.coverPhotoUrl)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GameDimens.SmallGap)) {
                Text(lot.title ?: lot.category, style = MaterialTheme.typography.titleMedium)
                Text("Rs "+lot.startingRate.toPlainString()+"/kg")
                Text(lot.weight.toPlainString()+" kg", style = MaterialTheme.typography.bodySmall)
                Text(if (lot.status != "OPEN") lot.status.label() else if (seconds <= 0) "Ended"
                    else if (seconds >= 86400) "${seconds / 86400}d ${seconds / 3600 % 24}h left"
                    else "${seconds / 3600}h ${seconds / 60 % 60}m left", style = MaterialTheme.typography.bodySmall)
                if (offers != null) {
                    Text("${offers.size} bids", style = MaterialTheme.typography.bodySmall)
                    offers.maxOfOrNull { it.rate }?.let { Text("Highest Rs ${it.toPlainString()}/kg", style = MaterialTheme.typography.bodySmall) }
                }
                if (lot.status == "OPEN" && seconds in 1..7199) BrickTag("Ending soon")
            }
        }
    }
}

@Composable
private fun HowItWorksRow(icon: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GameDimens.Gap)) {
        Box(Modifier.size(56.dp).border(GameDimens.Border, GameColors.Navy, GameShapes.small),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(32.dp), tint = GameColors.Navy)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SellingEmptyPreview() = com.tolmol.app.core.ui.theme.TolMolTheme {
    SellingContent(ScreenState(SellingData(emptyList(), 0, emptyMap())), {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun SellingListingsPreview() = com.tolmol.app.core.ui.theme.TolMolTheme {
    val lot = Listing("preview", "PAPER", BigDecimal("35"), BigDecimal("14"),
        Instant.now().plusSeconds(3600).toString(), "OPEN", "Bundled old newspapers", imageKey = "scrap_paper_01")
    SellingContent(ScreenState(SellingData(listOf(lot), 0, mapOf(lot.id to emptyList()))), {}, {}, {}, {})
}
