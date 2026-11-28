package com.tolmol.app.feature.discovery.presentation

import com.tolmol.app.core.ui.components.*
import com.tolmol.app.core.ui.theme.*
import com.tolmol.app.core.ui.components.TolMolTextField as OutlinedTextField
import com.tolmol.app.core.ui.components.BrickChip as FilterChip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolmol.app.R
import com.tolmol.app.core.ui.NativeIcon
import com.tolmol.app.core.ui.ScrapImage
import com.tolmol.app.core.ui.PixelButton
import com.tolmol.app.feature.discovery.domain.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.Duration
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(vm: DiscoveryViewModel, location: Instant?, startSearch: Boolean=false, watchedOnly: Boolean=false, onLot: (NearbyLot) -> Unit) {
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(vm,watchedOnly) { if(watchedOnly != vm.state.value.watchedOnly) vm.event(DiscoveryEvent.WatchedOnly) }
    LaunchedEffect(startSearch) { if(startSearch) { delay(200); focus.requestFocus(); keyboard?.show() } }
    val state by vm.state.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(vm, location) { vm.refresh() }
    LaunchedEffect(Unit) { while(true) { delay(1000); now=Instant.now() } }
    val sections=vm.sections(now)
    val selected=sections.find { it.id==state.section }
    BackHandler(state.section!=null) { vm.event(DiscoveryEvent.Section(null)) }
    val open: (NearbyLot)->Unit = { vm.event(DiscoveryEvent.Viewed(it.id)); onLot(it) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(state.query,{vm.event(DiscoveryEvent.Query(it))},Modifier.weight(1f).focusRequester(focus),
                placeholder={Text("Search scrap nearby")},singleLine=true,shape=RoundedCornerShape(24.dp),
                leadingIcon={
                    Canvas(Modifier.size(20.dp)) {
                        drawCircle(GameColors.Muted,radius=size.width*0.3f,center=Offset(size.width*0.4f,size.height*0.4f),style=Stroke(2.dp.toPx()))
                        drawLine(GameColors.Muted,Offset(size.width*0.63f,size.height*0.63f),Offset(size.width*0.95f,size.height*0.95f),2.dp.toPx())
                    }
                },
                trailingIcon={if(state.query.isNotEmpty()) IconButton({vm.event(DiscoveryEvent.Query(""))},
                    Modifier.semantics { contentDescription="Clear search" }) { Text("×") }})
            IconButton({vm.event(DiscoveryEvent.WatchedOnly)},Modifier.padding(start=4.dp).size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant,CircleShape)
                .semantics { contentDescription=if(state.watchedOnly) "Show all nearby lots" else "Show watchlist" }) {
                Heart(state.watchedOnly)
            }
        }
        LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            item { FilterChip(state.category==null,{vm.event(DiscoveryEvent.Category(null))},label={Text("All")}) }
            items(ScrapCategory.entries) { category ->
                FilterChip(state.category==category,{vm.event(DiscoveryEvent.Category(category))},label={Text(category.label)})
            }
        }
        PullToRefreshBox(state.loading,onRefresh={vm.refresh()},modifier=Modifier.weight(1f)) {
            if(state.section!=null) {
                Column {
                    Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                        TextButton({vm.event(DiscoveryEvent.Section(null))}) {Text("Back")}
                        Text(selected?.title ?: "Scrap lots",style=MaterialTheme.typography.headlineSmall)
                    }
                    if(state.error!=null) FeedError(state.error!!,vm::retry)
                    if(selected==null) FeedEmpty(vm::refresh,state.query.isNotEmpty() || state.category!=null)
                    else LazyVerticalGrid(GridCells.Adaptive(156.dp),contentPadding=PaddingValues(16.dp),
                        horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                        items(selected.lots,key={it.id}) { lot -> LotCard(lot,now,lot.id in state.watched,lot.id in state.pending,
                            {vm.event(DiscoveryEvent.Watch(lot.id))},{open(lot)},Modifier.fillMaxWidth()) }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp),
                    verticalArrangement=Arrangement.spacedBy(24.dp)) {
                    if(state.error!=null) item { FeedError(state.error!!,vm::retry) }
                    if(!state.loaded && (state.loading || state.error==null)) {
                        items(2) { SkeletonSection() }
                    } else if(sections.isEmpty() && state.error==null) {
                        item { FeedEmpty(vm::refresh,state.query.isNotEmpty() || state.category!=null || state.watchedOnly) }
                    } else items(sections,key={it.id}) { section ->
                        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                                OutlinedPixelText(section.title,Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall)
                                YellowUnderlineLink("See all",{vm.event(DiscoveryEvent.Section(section.id))})
                            }
                            LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                items(section.lots.take(12),key={it.id}) { lot ->
                                    LotCard(lot,now,lot.id in state.watched,lot.id in state.pending,
                                        {vm.event(DiscoveryEvent.Watch(lot.id))},{open(lot)},Modifier.width(196.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LotCard(lot: NearbyLot,now: Instant,watched: Boolean,pending: Boolean,watch: ()->Unit,open: ()->Unit,modifier: Modifier) {
    BrickCard(modifier,onClick=open,contentPadding=PaddingValues(start=8.dp,end=12.dp,top=18.dp,bottom=12.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).border(GameDimens.Border,GameColors.Navy,RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)) {
            ScrapImage(lot.imageKey,ScrapCategory.of(lot.category).name,Modifier.matchParentSize(),lot.coverPhotoUrl)
            if(endingSoon(lot,now)) BrickTag("Ending soon",Modifier.align(Alignment.BottomStart).padding(8.dp))
            IconButton(watch,Modifier.align(Alignment.TopEnd).padding(6.dp).size(44.dp)
                .background(GameColors.Paper,CircleShape).border(GameDimens.Border,GameColors.Navy,CircleShape).semantics {
                    contentDescription=if(watched) "Unwatch ${lot.title}" else "Watch ${lot.title}"
                    stateDescription=if(watched) "Watched" else "Not watched"
                },enabled=!pending) {Heart(watched)}
        }
        Text(lot.title,style=MaterialTheme.typography.bodyMedium,maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis)
        Text("₹${lot.startingRate.stripTrailingZeros().toPlainString()}/kg",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
        Text("${lot.weight.stripTrailingZeros().toPlainString()} kg · ${lot.distanceKm?.let { String.format(Locale.ENGLISH,"%.1f km",it) } ?: "Nearby"}",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(timeLeft(lot.deadline,now),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
private fun Heart(filled: Boolean) {
    val color=if(filled) MaterialTheme.colorScheme.primary else GameColors.Navy
    CompositionLocalProvider(LocalContentColor provides color) {
        NativeIcon(if(filled) R.drawable.ic_ui_heart_filled else R.drawable.ic_ui_heart,null)
    }
}
private fun timeLeft(deadline: String,now: Instant): String {
    val minutes=runCatching { Duration.between(now,Instant.parse(deadline)).toMinutes().coerceAtLeast(0) }.getOrDefault(0)
    return when {minutes>=1440 -> "${minutes/1440}d ${minutes%1440/60}h left"; minutes>=60 -> "${minutes/60}h ${minutes%60}m left";else -> "${minutes}m left"}
}
@Composable
private fun FeedError(message: String,retry: ()->Unit) {
    BrickCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(message,color=MaterialTheme.colorScheme.error)
        BrickSecondaryButton(retry) {Text("Retry")}
    }
}
@Composable
private fun FeedEmpty(refresh: ()->Unit,filtered: Boolean) {
    EmptyState("No scrap nearby yet",
        if(filtered) "Try another search or category." else "New lots within 20 km will appear here.",refresh)
}
@Composable
private fun SkeletonSection() {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.padding(horizontal=16.dp).size(160.dp,24.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        LazyRow(contentPadding=PaddingValues(16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            items(3) {
                Column(Modifier.width(196.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    SkeletonBrick(Modifier.fillMaxWidth().aspectRatio(1f))
                    Box(Modifier.size(140.dp,18.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Box(Modifier.size(100.dp,18.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }
        }
    }
}
