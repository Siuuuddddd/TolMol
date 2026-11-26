package com.tolmol.app.feature.deal.presentation

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
import com.tolmol.app.core.common.LiveUpdate
import com.tolmol.app.core.ui.*
import com.tolmol.app.feature.deal.domain.*
import java.math.BigDecimal

class DealsViewModel(private val deals: DealUseCase) : ScreenViewModel<List<Deal>>() {
    val live = deals.live
    fun refresh() = load { deals.mine() }
}

class DealViewModel(private val deals: DealUseCase) : ScreenViewModel<Deal>() {
    val live = deals.live
    fun refresh(id: String) = load { deals.deal(id) }
    fun inspect(id: String, weight: BigDecimal) = load { deals.inspect(id, weight) }
    fun propose(id: String, total: BigDecimal) = load { deals.propose(id, total) }
    fun confirm(id: String, revision: Long) = load { deals.confirm(id, revision) }
    fun cancel(id: String) = load { deals.cancel(id) }
}

@Composable
fun DealsScreen(vm: DealsViewModel, user: String, seller: Boolean, open: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.refresh()
        vm.live.collect { if (it is LiveUpdate.Connected || it is LiveUpdate.Event && (it.type.startsWith("DEAL_") || it.type == "AUCTION_AWARDED")) vm.refresh() }
    }
    val items = state.value?.filter { (if (seller) it.seller else it.buyer) == user }
    Screen("Deals") {
        ScreenFeedback(state) { vm.refresh() }
        if (items?.isEmpty() == true) EmptyState("No deals yet", "Awarded lots appear here.")
        for (item in items.orEmpty()) {
            RowCard(item.state.name.label(), "Lot ${item.listing.take(8)} · ₹${item.rate.toPlainString()}/kg") { open(item.listing) }
        }
    }
}

@Composable
fun DealScreen(vm: DealViewModel, id: String, user: String) {
    val state by vm.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var confirmCancel by remember { mutableStateOf(false) }
    LaunchedEffect(vm, id) {
        vm.refresh(id)
        vm.live.collect { if (it is LiveUpdate.Connected || it is LiveUpdate.Event && it.aggregate == id) vm.refresh(id) }
    }
    val deal = state.value
    LaunchedEffect(deal?.revision) { input = "" }
    Screen("Pickup and settlement") {
        ScreenFeedback(state) { vm.refresh(id) }
        if (deal != null) {
            BrickCard(Modifier.fillMaxWidth()) {
            Text(deal.state.name.label(), style = MaterialTheme.typography.titleLarge,
                color=when(deal.state) {DealState.COMPLETED->GameColors.Green;DealState.WEIGHT_DISPUTE->GameColors.RedShadow;else->GameColors.Navy})
            Text("Pickup coordinates: ${deal.pickupLatitude}, ${deal.pickupLongitude}")
            Text("Agreed rate: ₹${deal.rate.toPlainString()}/kg")
            Text("Estimated weight: ${deal.estimatedWeight.toPlainString()} kg")
            deal.actualWeight?.let { Text("Measured weight: ${it.toPlainString()} kg") }
            deal.amount?.let { Text("Settlement total: ₹${it.toPlainString()}") }
            deal.relisted?.let { Text("Replacement listing: $it") }
            }
            val inspecting = deal.state == DealState.PENDING_PICKUP && deal.actualWeight == null && deal.buyer == user
            val disputing = deal.state == DealState.WEIGHT_DISPUTE
            val entered = input.toBigDecimalOrNull()
            if (inspecting || disputing) {
                OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(),
                    label = { Text(if (inspecting) "Measured kg" else "Revised total in ₹") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Button({
                    if (entered != null) {
                        if (inspecting) vm.inspect(id, entered) else vm.propose(id, entered)
                    }
                }, enabled = !state.loading && entered?.signum() == 1) {
                    Text(if (inspecting) "Record weight" else "Propose total")
                }
            }
            if ((disputing || deal.state == DealState.PENDING_PICKUP) && deal.amount != null) {
                if (deal.proposedBy == user) Text("Waiting for the other participant to confirm.")
                else Button({ vm.confirm(id, deal.revision) }, enabled = !state.loading) { Text("Confirm settlement") }
            }
            if (disputing) OutlinedButton({ confirmCancel = true }, enabled = !state.loading) { Text("Cancel and relist") }
        }
    }
    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Cancel this deal?") },
        text = { Text("The lot will be relisted with the measured weight and a new deadline.") },
        confirmButton = { Button({ confirmCancel = false; vm.cancel(id) }) { Text("Cancel and relist") } },
        dismissButton = { TextButton({ confirmCancel = false }) { Text("Keep negotiating") } },
    )
}
