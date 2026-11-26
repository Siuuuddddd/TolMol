package com.tolmol.app.feature.bidding.presentation

import com.tolmol.app.core.ui.components.*
import com.tolmol.app.core.ui.theme.*
import com.tolmol.app.core.ui.components.TolMolTextField as OutlinedTextField
import com.tolmol.app.core.ui.components.PixelDialog as AlertDialog
import com.tolmol.app.core.ui.components.BrickButton as Button
import com.tolmol.app.core.ui.components.BrickSecondaryButton as OutlinedButton

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolmol.app.R
import com.tolmol.app.core.ui.*
import com.tolmol.app.feature.discovery.domain.*
import com.tolmol.app.feature.discovery.presentation.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiddingScreen(vm: BiddingViewModel, id: String, category: String, weight: String, opening: String,
    deadline: String, openChat: (() -> Unit)?, openDeal: (() -> Unit)?, details: LotDetailViewModel,
    recommendations: DiscoveryViewModel, onBack: ()->Unit, onSearch: ()->Unit,
    onWatchlist: ()->Unit, onRelatedLot: (NearbyLot)->Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val detail by details.state.collectAsStateWithLifecycle()
    val related by recommendations.state.collectAsStateWithLifecycle()
    val lot=detail.lot
    var now by remember { mutableStateOf(Instant.now()) }
    var showBid by rememberSaveable(id) { mutableStateOf(false) }
    var bidPending by remember { mutableStateOf(false) }
    var rate by rememberSaveable(id) { mutableStateOf("") }
    var description by remember { mutableStateOf(false) }
    var productInfo by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val context=LocalContext.current
    val clipboard=LocalClipboardManager.current
    val end=runCatching { Instant.parse(lot?.deadline ?: deadline) }.getOrDefault(now)
    val auction=state.value
    val currentRate=auction?.highest ?: lot?.startingRate ?: opening.toBigDecimal()
    val kilograms=lot?.weight ?: weight.toBigDecimal()
    val isOpen=auction?.status=="OPEN" && now.isBefore(end)
    val title=lot?.title ?: category
    val member=lot?.sellerMemberSince?.let { value -> runCatching {
        DateTimeFormatter.ofPattern("MMM yyyy",Locale.ENGLISH).withZone(ZoneOffset.UTC).format(Instant.parse(value))
    }.getOrNull() }
    LaunchedEffect(details,id) {details.refresh(id)}
    LaunchedEffect(recommendations,id) {recommendations.refresh()}
    LaunchedEffect(id) {while(true) {now=Instant.now();delay(1000)}}
    DisposableEffect(vm,id) {vm.observe(id);onDispose {vm.stop(id)}}
    LaunchedEffect(state.loading,state.error,bidPending) {
        if(bidPending && !state.loading) {bidPending=false;if(state.error==null) showBid=false}
    }
    Column(Modifier.fillMaxSize()) {
        SteelBar {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onBack) {NativeIcon(R.drawable.ic_ui_back,"Back")}
                Text("Item",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                DetailToolbarButton(R.drawable.ic_ui_search,"Search",onSearch)
                DetailToolbarButton(R.drawable.ic_ui_heart,"Watchlist",onWatchlist)
                DetailToolbarButton(R.drawable.ic_ui_share,"Share") {
                    val share=Intent(Intent.ACTION_SEND).apply {
                        type="text/plain"
                        putExtra(Intent.EXTRA_TEXT,"$title\nTolMol · ₹${currentRate.stripTrailingZeros().toPlainString()}/kg · ${kilograms.stripTrailingZeros().toPlainString()} kg\nListing: $id")
                    }
                    context.startActivity(Intent.createChooser(share,"Share scrap lot"))
                }
                Box {
                    DetailToolbarButton(R.drawable.ic_ui_more,"More options") {menu=true}
                    DropdownMenu(menu,{menu=false}) {
                        DropdownMenuItem(text={Text("Full description")},onClick={menu=false;description=true})
                        DropdownMenuItem(text={Text("Copy listing ID")},onClick={menu=false;clipboard.setText(AnnotatedString(id))})
                    }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if(detail.loading) SkeletonBrick(Modifier.fillMaxWidth())
            ScrapPhotoGallery(lot?.imageKey,lot?.category ?: category,Modifier.fillMaxWidth(),fullBleed=true,
                watched=detail.watched,watching=detail.watching,onWatch=if(lot!=null) ({details.watch(id)}) else null)
            Column(Modifier.padding(horizontal=16.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                if(detail.error!=null) {
                    Text(detail.error!!,color=MaterialTheme.colorScheme.error)
                    TextButton({details.refresh(id)}) {Text("Retry details")}
                }
                OutlinedPixelText(title,style=MaterialTheme.typography.headlineSmall)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(48.dp)) {
                        Box(contentAlignment=Alignment.Center) {Text(lot?.sellerDisplayName?.take(1) ?: "T",style=MaterialTheme.typography.headlineSmall)}
                    }
                    Column(Modifier.weight(1f)) {
                        Text(lot?.sellerDisplayName ?: "Loading seller…",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Bold,
                            maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(member?.let {"Member since $it"} ?: "TolMol member",style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                    }
                    if(openChat!=null) OutlinedButton(openChat,shape=RoundedCornerShape(24.dp),contentPadding=PaddingValues(horizontal=14.dp)) {Text("Message",style=MaterialTheme.typography.labelMedium)}
                }
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("₹${currentRate.stripTrailingZeros().toPlainString()}/kg",style=MaterialTheme.typography.titleLarge.copy(fontSize=GameDimens.Price))
                    Text(if(auction?.highest!=null) "Highest bid · Opening ₹$opening/kg" else "Opening rate · No bids yet",
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Estimated lot value ₹${(kilograms*currentRate).stripTrailingZeros().toPlainString()}",style=MaterialTheme.typography.bodyMedium)
                }
                DetailFact("Pickup",lot?.distanceKm?.let {String.format(Locale.ENGLISH,"About %.1f km away",it)} ?: "Local collection")
                DetailFact("Lot weight","${kilograms.stripTrailingZeros().toPlainString()} kg estimated")
                if(!showBid) ScreenFeedback(state.copy(error=state.error?.let {bidError(it,now)})) {vm.refresh(id)}
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    if(isOpen) Button({
                        if(rate.isBlank()) rate=(currentRate+java.math.BigDecimal.ONE).stripTrailingZeros().toPlainString()
                        showBid=true
                    },Modifier.fillMaxWidth().height(GameDimens.Button),shape=RoundedCornerShape(28.dp),enabled=!state.loading) {Text("Place a bid")}
                    else if(openDeal!=null && auction?.status=="AWARDED") Button(openDeal,Modifier.fillMaxWidth().height(GameDimens.Button),shape=RoundedCornerShape(28.dp)) {Text("View pickup deal")}
                    else if(auction!=null) Text(if(now.isBefore(end)) auction.status.label() else "Auction ended",style=MaterialTheme.typography.titleMedium)
                    OutlinedButton({details.watch(id)},Modifier.fillMaxWidth().height(GameDimens.Button),shape=RoundedCornerShape(28.dp),
                        enabled=lot!=null && !detail.watching && (isOpen || detail.watched)) {
                        NativeIcon(if(detail.watched) R.drawable.ic_ui_heart_filled else R.drawable.ic_ui_heart,null)
                        Spacer(Modifier.width(8.dp));Text(if(detail.watched) "Added to watchlist" else "Add to watchlist")
                    }
                    if(openChat!=null) OutlinedButton(openChat,Modifier.fillMaxWidth().height(GameDimens.Button),shape=RoundedCornerShape(28.dp)) {Text("Message seller")}
                }
                BrickCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        NativeIcon(R.drawable.ic_ui_info,null)
                        Column {
                            Text(if(isOpen) "Live auction" else "Auction update",fontWeight=FontWeight.Bold)
                            Text(if(now.isBefore(end)) "Ends in ${remaining(end,now)}. Bids are per kg." else "The auction deadline has passed.",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                OutlinedPixelText("Item specifics")
                DetailFact("Category",(lot?.category ?: category).lowercase().replaceFirstChar {it.titlecase()})
                DetailFact("Quantity","1 scrap lot")
                DetailFact("Weight","${kilograms.stripTrailingZeros().toPlainString()} kg")
                DetailFact("Status",auction?.status?.label() ?: "Loading…")
                DetailFact("Item number",id.take(8))
                DetailLink("See full description") {description=true}
                OutlinedPixelText("Product details")
                DetailLink("See product details") {productInfo=true}
            }
            val same=related.lots.filter {it.id!=id && ScrapCategory.of(it.category)==ScrapCategory.of(category)}
            val other=if(same.isNotEmpty()) same else related.lots.filter {it.id!=id}
            if(related.loading || related.error!=null || other.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
                    OutlinedPixelText("More scrap nearby",Modifier.weight(1f))
                    OutlinedButton(onSearch,shape=RoundedCornerShape(24.dp),contentPadding=PaddingValues(horizontal=12.dp)) {
                        Text("See all",style=MaterialTheme.typography.labelMedium);Spacer(Modifier.width(6.dp));NativeIcon(R.drawable.ic_ui_chevron,null,Modifier.size(18.dp))
                    }
                }
                if(related.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=16.dp))
                if(related.error!=null) Column(Modifier.padding(horizontal=16.dp)) {
                    Text(related.error!!,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                    TextButton({recommendations.retry()}) {Text("Retry")}
                }
                LazyRow(contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(other.take(8),key={it.id}) { item ->
                        LotCard(item,now,item.id in related.watched,item.id in related.pending,
                            {recommendations.event(DiscoveryEvent.Watch(item.id))},{onRelatedLot(item)},Modifier.width(196.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
                BrickWallFooter(Modifier.fillMaxWidth())
            }
        }
    }
    if(showBid) ModalBottomSheet(onDismissRequest={if(!state.loading) showBid=false},containerColor=MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal=20.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Place your bid",style=MaterialTheme.typography.titleLarge)
            Text("Current rate ₹${currentRate.stripTrailingZeros().toPlainString()}/kg · $kilograms kg estimated",style=MaterialTheme.typography.bodySmall)
            OutlinedTextField(rate,{rate=it},Modifier.fillMaxWidth(),label={Text("Your rate in ₹ per kg")},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
            ScreenFeedback(state.copy(error=state.error?.let {bidError(it,now)})) {
                rate.toBigDecimalOrNull()?.let {bidPending=true;vm.place(id,it)}
            }
            Button({rate.toBigDecimalOrNull()?.let {bidPending=true;vm.place(id,it)}},Modifier.fillMaxWidth().height(GameDimens.Button),
                shape=RoundedCornerShape(28.dp),enabled=!state.loading && rate.toBigDecimalOrNull()?.signum()==1 && isOpen) {Text("Confirm bid")}
        }
    }
    if(description || productInfo) AlertDialog(onDismissRequest={description=false;productInfo=false},
        title={Text(if(description) "Full description" else "Product details")},
        text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(description) Text(lot?.description?.ifBlank {"Inspect the lot and confirm its condition at pickup."} ?: "Details are still loading. Please retry.")
            else {Text(title);Text("Listing ID: $id");Text("Estimated weight: $kilograms kg");Text("Opening rate: ₹$opening/kg");Text("Pickup details are available to deal participants after an offer is accepted.")}
        }},confirmButton={TextButton({description=false;productInfo=false}) {Text("Done")}})
}

@Composable
private fun DetailToolbarButton(icon: Int,label: String,click: ()->Unit) {
    IconButton(click,Modifier.padding(2.dp).size(40.dp).background(GameColors.Tile,CircleShape)) {NativeIcon(icon,label)}
}
@Composable
private fun DetailFact(label: String,value: String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(label,Modifier.width(112.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
    }
}
@Composable
private fun DetailLink(label: String,click: ()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=click).padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,textDecoration=androidx.compose.ui.text.style.TextDecoration.Underline)
        NativeIcon(R.drawable.ic_ui_chevron,null)
    }
}
