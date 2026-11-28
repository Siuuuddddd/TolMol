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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant

data class SellingData(
    val listings: List<Listing>,
    val orders: Int,
    val offers: Map<String, List<Offer>>,
)

class ListingViewModel(
    private val listings: ListingUseCase,
    private val deals: com.tolmol.app.feature.deal.domain.DealUseCase,
) : ScreenViewModel<SellingData>() {
    fun refresh() = load {
        val lots = mutableListOf<Listing>()
        var after: String? = null
        do {
            val page = listings.mine(after)
            lots.addAll(page)
            val next = page.lastOrNull()?.id
            val more = page.size >= 25 && next != after
            after = next
        } while (more)
        val ids = lots.map { it.id }.toSet()
        val orders = deals.mine().count { it.listing in ids }
        val offers = lots.filter { it.status == "OPEN" && Instant.parse(it.deadline).isAfter(Instant.now()) }
            .associate { it.id to listings.offers(it.id) }
        SellingData(lots.distinctBy { it.id }, orders, offers)
    }
}

class PublishViewModel(private val listings: ListingUseCase,private val photos: UploadListingPhotosUseCase) : ScreenViewModel<Listing>() {
    var selected by mutableStateOf<List<PreparedPhoto>>(emptyList())
        private set
    var preparing by mutableStateOf(false)
        private set
    var photoError by mutableStateOf<String?>(null)
        private set
    var progress by mutableStateOf<String?>(null)
        private set
    private var created: Listing? = null
    private val uploaded = mutableSetOf<String>()
    fun add(sources: List<String>) {
        if(preparing || state.value.loading || created!=null) return
        preparing=true
        viewModelScope.launch {
            try {
                photoError=null
                sources.take(5-selected.size).forEach { source -> selected=selected+photos.prepare(source) }
            } catch(e: kotlinx.coroutines.CancellationException) { throw e }
            catch(e: Exception) { photoError=e.message ?: "Unable to prepare photo. Try another image." }
            finally { preparing=false }
        }
    }
    fun remove(key: String) { if(created==null && !state.value.loading) selected=selected.filterNot { it.key==key } }
    fun cover(key: String) { if(created==null && !state.value.loading) selected=selected.sortedBy { it.key!=key } }
    fun cameraDenied() { photoError="Camera access lets you take a scrap photo. You can also choose from gallery." }
    fun publish(category: String, weight: BigDecimal, opening: BigDecimal, hours: Long,title: String,description: String) = load {
        val lot=created ?: listings.publish(category.trim(),weight,opening,Instant.now().plusSeconds(hours*3600).toString(),title.trim(),description.trim()).also { created=it }
        try {
            selected.forEachIndexed { index,photo ->
                progress="Uploading photo "+(index+1)+" of "+selected.size
                if(photo.key !in uploaded) { photos.upload(lot.id,photo);uploaded.add(photo.key) }
            }
            lot
        } finally { progress=null }
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
fun PublishScreen(vm: PublishViewModel, published: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var details by rememberSaveable { mutableStateOf(false) }
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
        if(!details) {
            PhotoPickerSection(vm)
            BrickButton({details=true}, Modifier.fillMaxWidth(), enabled=!vm.preparing) { Text("Continue to details") }
            return@Screen
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("DETAILS",style=MaterialTheme.typography.headlineSmall)
            Text("STEP 2 OF 2",style=MaterialTheme.typography.labelSmall)
        }
        TextButton({details=false},enabled=!state.loading) {Text("Edit photos")}

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
        vm.progress?.let { Text(it) }
        ScreenFeedback(state, submit)
        Button(submit, enabled = valid && !state.loading && !vm.preparing) { Text("Publish listing") }
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
