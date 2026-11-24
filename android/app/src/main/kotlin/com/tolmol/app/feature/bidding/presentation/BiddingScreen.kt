package com.tolmol.app.feature.bidding.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tolmol.app.core.common.LiveUpdate
import com.tolmol.app.core.ui.*
import com.tolmol.app.feature.bidding.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BiddingViewModel @Inject constructor(private val bids: BiddingUseCase) : ScreenViewModel<Auction>() {
    private var subscription: Job? = null
    private var pending: PendingBid? = null

    fun observe(id: String) {
        subscription?.cancel()
        subscription = viewModelScope.launch {
            bids.live.collect { update ->
                state.first { !it.loading }
                when (update) {
                    LiveUpdate.Connected -> load { bids.subscribe(id) }
                    LiveUpdate.Disconnected -> refresh(id)
                    is LiveUpdate.Event -> if (update.aggregate == id && update.data.containsKey("version")) refresh(id)
                }
            }
        }
    }

    fun stop(id: String) {
        subscription?.cancel()
        viewModelScope.launch { runCatching { bids.unsubscribe(id) } }
    }

    fun refresh(id: String) = load { bids.auction(id) }

    fun place(id: String, rate: BigDecimal) = load {
        val attempt = pending?.takeIf { it.listing == id && it.rate.compareTo(rate) == 0 }
            ?: PendingBid(id, rate, UUID.randomUUID().toString()).also { pending = it }
        bids.place(id, rate, attempt.key)
        pending = null
        bids.auction(id)
    }
}

data class PendingBid(val listing: String, val rate: BigDecimal, val key: String)

fun remaining(end: Instant, now: Instant): String {
    val seconds = Duration.between(now, end).seconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${seconds % 60}s"
}

fun bidError(message: String, now: Instant): String {
    val prefix = "Next bid allowed at "
    if (!message.startsWith(prefix)) return message
    val next = runCatching { Instant.parse(message.removePrefix(prefix)) }.getOrNull() ?: return message
    return if (now.isBefore(next)) "You can bid with this seller again in ${remaining(next, now)}."
        else "The cooldown has ended. You can bid again."
}

@Composable
fun BiddingScreen(vm: BiddingViewModel, id: String, category: String, weight: String, opening: String,
                  deadline: String, openChat: (() -> Unit)?, openDeal: (() -> Unit)?) {
    val state by vm.state.collectAsStateWithLifecycle()
    var rate by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableStateOf(Instant.now()) }
    val end = remember(deadline) { Instant.parse(deadline) }
    DisposableEffect(id) {
        vm.observe(id)
        onDispose { vm.stop(id) }
    }
    LaunchedEffect(id) {
        while (true) {
            now = Instant.now()
            delay(1000)
        }
    }
    val auction = state.value
    Screen(category) {
        Text("$weight kg estimated")
        Text("Opening rate ₹$opening/kg")
        Text("Highest bid: ${auction?.highest?.let { "₹${it.toPlainString()}/kg" } ?: "No bids yet"}")
        Text(if (now.isBefore(end)) "Time left ${remaining(end, now)}" else "Auction deadline passed")
        auction?.let { Text(it.status.label()) }
        ScreenFeedback(state.copy(error = state.error?.let { bidError(it, now) })) { vm.refresh(id) }
        val amount = rate.toBigDecimalOrNull()
        if (auction?.status == "OPEN" && now.isBefore(end)) {
            OutlinedTextField(rate, { rate = it }, Modifier.fillMaxWidth(), label = { Text("Your bid in ₹ per kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            Button({ amount?.let { vm.place(id, it) } }, enabled = !state.loading && amount?.signum() == 1) { Text("Place bid") }
            if (openChat != null) OutlinedButton(openChat) { Text("Message seller") }
        }
        if (openDeal != null && (auction?.status == "AWARDED" || !now.isBefore(end))) OutlinedButton(openDeal) { Text("Check pickup deal") }
    }
}

@HiltViewModel
class MyBidsViewModel @Inject constructor(private val bids: BiddingUseCase) : ScreenViewModel<List<MyBid>>() {
    val live = bids.live
    fun refresh() = load { bids.mine() }
    fun more() = load {
        val current = state.value.value.orEmpty()
        current + bids.mine(current.lastOrNull()?.id)
    }
}

@Composable
fun MyBidsScreen(vm: MyBidsViewModel, openDeal: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.refresh()
        vm.live.collect { if (it is LiveUpdate.Connected || it is LiveUpdate.Event && it.type.startsWith("BID_")) vm.refresh() }
    }
    Screen("My bids") {
        ScreenFeedback(state) { vm.refresh() }
        if (state.value?.isEmpty() == true) Text("You have not placed any bids.")
        for (bid in state.value.orEmpty()) {
            if (bid.status == "WON") RowCard("₹${bid.rate.toPlainString()}/kg · Won", bid.createdAt) { openDeal(bid.listing) }
            else {
                Text("₹${bid.rate.toPlainString()}/kg · ${bid.status.label()}", style = MaterialTheme.typography.titleMedium)
                Text(bid.createdAt)
            }
        }
        if (state.value.orEmpty().size >= 25) TextButton({ vm.more() }, enabled = !state.loading) { Text("Load more") }
    }
}
