package com.tolmol.app.feature.discovery.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolmol.app.core.ui.*
import com.tolmol.app.feature.discovery.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject

data class NearbyLots(val lots: List<NearbyLot>, val watched: Set<String>)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(private val discovery: DiscoveryUseCase) : ScreenViewModel<NearbyLots>() {
    fun refresh() = load { fetch() }

    fun watch(id: String, watched: Boolean) = load {
        discovery.watch(id, watched)
        fetch()
    }

    private suspend fun fetch(): NearbyLots = NearbyLots(discovery.nearby(), discovery.watched().map { it.id }.toSet())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(vm: DiscoveryViewModel, location: Instant?, onLot: (NearbyLot) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var watchlist by remember { mutableStateOf(false) }
    LaunchedEffect(location) { vm.refresh() }

    PullToRefreshBox(isRefreshing = state.loading, onRefresh = { vm.refresh() }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(if (watchlist) "Watchlist" else "Nearby scrap", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!watchlist, { watchlist = false }, label = { Text("All nearby") })
                    FilterChip(watchlist, { watchlist = true }, label = { Text("Watched") })
                }
                ScreenFeedback(state) { vm.refresh() }
            }
            val current = state.value
            val lots = current?.lots.orEmpty().filter { !watchlist || it.id in current?.watched.orEmpty() }
            if (current != null && lots.isEmpty()) item { Text(if (watchlist) "No watched lots nearby." else "No open lots within 20 km.") }
            items(lots, key = { it.id }) { lot ->
                val watched = lot.id in current?.watched.orEmpty()
                RowCard(lot.category, "${lot.weight.toPlainString()} kg estimated · Opens at ₹${lot.startingRate.toPlainString()}/kg") { onLot(lot) }
                TextButton({ vm.watch(lot.id, !watched) }, enabled = !state.loading) { Text(if (watched) "Unwatch" else "Watch") }
            }
        }
    }
}
