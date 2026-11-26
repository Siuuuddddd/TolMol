package com.tolmol.app.feature.listing.presentation

import com.tolmol.app.core.ui.components.EmptyState
import com.tolmol.app.core.ui.components.*
import com.tolmol.app.core.ui.theme.*
import com.tolmol.app.core.ui.components.TolMolTextField as OutlinedTextField
import com.tolmol.app.core.ui.components.PixelDialog as AlertDialog

import com.tolmol.app.core.ui.PixelButton as Button
import com.tolmol.app.core.ui.PixelOutlinedButton as OutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolmol.app.core.ui.*
import com.tolmol.app.feature.listing.domain.*
import java.math.BigDecimal
import java.time.Instant

class ListingViewModel(private val listings: ListingUseCase) : ScreenViewModel<List<Listing>>() {
    fun refresh() = load { listings.mine() }
    fun more() = load {
        val current = state.value.value.orEmpty()
        current + listings.mine(current.lastOrNull()?.id)
    }
}

class PublishViewModel(private val listings: ListingUseCase) : ScreenViewModel<Listing>() {
    fun publish(category: String, weight: BigDecimal, opening: BigDecimal, hours: Long,title: String,description: String) = load {
        listings.publish(category.trim(), weight, opening, Instant.now().plusSeconds(hours * 3600).toString(),title.trim(),description.trim())
    }
}

class OffersViewModel(private val listings: ListingUseCase) : ScreenViewModel<List<Offer>>() {
    var awarded by mutableStateOf(false)
        private set

    fun refresh(id: String) = load { listings.offers(id) }
    fun accept(id: String, bid: String) = load {
        listings.accept(id, bid)
        awarded = true
        listings.offers(id)
    }
}

@Composable
fun MyListingsScreen(vm: ListingViewModel, publish: () -> Unit, open: (Listing) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.refresh() }
    Screen("My listings") {
        Button(publish) { Text("Publish a listing") }
        ScreenFeedback(state) { vm.refresh() }
        if (state.value?.isEmpty() == true) EmptyState("No listings yet", "Your published scrap lots appear here.")
        for (listing in state.value.orEmpty()) {
            RowCard(listing.title ?: listing.category, "${listing.weight.toPlainString()} kg · ${listing.status.label()}") { open(listing) }
        }
        if (state.value.orEmpty().size >= 25) TextButton({ vm.more() }, enabled = !state.loading) { Text("Load more") }
    }
}

@Composable
fun PublishScreen(vm: PublishViewModel, published: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf("OTHER") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryOpen by remember { mutableStateOf(false) }
    var weight by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("24") }
    val kg = weight.toBigDecimalOrNull()
    val opening = rate.toBigDecimalOrNull()
    val duration = hours.toLongOrNull()
    val valid = title.trim().length in 1..160 && description.length<=2000 && category.isNotBlank() && kg?.signum() == 1 && opening?.signum() == 1 && duration != null && duration in 1L..8760L
    val submit = {
        if (valid) vm.publish(category, requireNotNull(kg), requireNotNull(opening), requireNotNull(duration),title,description)
    }
    LaunchedEffect(state.value) { if (state.value != null) published() }
    Screen("Publish scrap") {
        OutlinedTextField(title,{title=it.take(160)},Modifier.fillMaxWidth(),label={Text("Title")},singleLine=true)
        OutlinedTextField(description,{description=it.take(2000)},Modifier.fillMaxWidth(),label={Text("Description")},minLines=3,maxLines=6)
        Box {
            OutlinedButton({categoryOpen=true}) {Text("Category: "+category.lowercase().replaceFirstChar { it.titlecase() })}
            DropdownMenu(categoryOpen,{categoryOpen=false}) {
                com.tolmol.app.feature.discovery.domain.ScrapCategory.entries.forEach { item ->
                    DropdownMenuItem(text={Text(item.label)},onClick={category=item.name;categoryOpen=false})
                }
            }
        }
        DecimalField(weight, { weight = it }, "Estimated kg")
        DecimalField(rate, { rate = it }, "Opening rate in ₹ per kg")
        OutlinedTextField(hours, { hours = it }, Modifier.fillMaxWidth(), label = { Text("Auction deadline in hours") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        Text("Your current seller location is used for pickup.")
        ScreenFeedback(state, submit)
        Button(submit, enabled = valid && !state.loading) { Text("Publish listing") }
    }
}

@Composable
private fun DecimalField(value: String, update: (String) -> Unit, title: String) {
    OutlinedTextField(value, update, Modifier.fillMaxWidth(), label = { Text(title) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
}

@Composable
fun OffersScreen(vm: OffersViewModel, id: String, category: String, status: String, deadline: String, chat: (() -> Unit)?, deal: (() -> Unit)?) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<Offer?>(null) }
    LaunchedEffect(vm, id) { vm.refresh(id) }
    val open = status == "OPEN" && !vm.awarded && Instant.now().isBefore(Instant.parse(deadline))
    Screen(category) {
        if (chat != null) OutlinedButton(chat) { Text("Conversations") }
        if (deal != null && !open && status != "EXPIRED") OutlinedButton(deal) { Text("Pickup deal") }
        ScreenFeedback(state) { vm.refresh(id) }
        if (state.value?.isEmpty() == true) EmptyState("Waiting for offers", "No bids on this listing yet.")
        for (offer in state.value.orEmpty()) {
            BrickCard(Modifier.fillMaxWidth()) {
            Text("₹${offer.rate.toPlainString()}/kg", style = MaterialTheme.typography.titleMedium)
            Text("Buyer ${offer.buyer.take(8)} · ${offer.createdAt}")
            if (open) Button({ confirm = offer }, enabled = !state.loading) { Text("Accept this bid") }
            }
        }
    }
    confirm?.let { offer ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text("Accept this bid?") },
            text = { Text("Award the lot at ₹${offer.rate.toPlainString()}/kg and close bidding.") },
            confirmButton = { Button({ confirm = null; vm.accept(id, offer.id) }) { Text("Accept bid") } },
            dismissButton = { TextButton({ confirm = null }) { Text("Back") } })
    }
}
