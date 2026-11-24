package com.tolmol.app.feature.notification.presentation

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolmol.app.core.common.LiveUpdate
import com.tolmol.app.core.ui.*
import com.tolmol.app.feature.notification.domain.*

class NotificationViewModel(private val inbox: NotificationUseCase) : ScreenViewModel<List<Alert>>() {
    val live = inbox.live
    fun refresh() = load { inbox.inbox() }
    fun read(id: String) = load { inbox.read(id); inbox.inbox() }
    fun readAll() = load { inbox.readAll(); inbox.inbox() }
    fun more() = load {
        val current = state.value.value.orEmpty()
        val before = current.lastOrNull()?.cursor
        if (before == null) current else current + inbox.inbox(before)
    }
}

@Composable
fun InboxScreen(vm: NotificationViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.refresh()
        vm.live.collect { if (it is LiveUpdate.Event || it is LiveUpdate.Connected) vm.refresh() }
    }
    Screen("Alerts") {
        ScreenFeedback(state) { vm.refresh() }
        if (state.value?.isEmpty() == true) Text("No alerts yet.")
        if (state.value.orEmpty().any { !it.read }) TextButton({ vm.readAll() }, enabled = !state.loading) { Text("Mark all read") }
        for (alert in state.value.orEmpty()) {
            if (alert.read) Text(alert.type.replace('.', '_').label())
            else RowCard(alert.type.replace('.', '_').label(), "Unread · Tap to mark read") { vm.read(alert.eventId) }
        }
        if (state.value.orEmpty().size >= 100) TextButton({ vm.more() }, enabled = !state.loading) { Text("Load more") }
    }
}
