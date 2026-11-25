package com.tolmol.app.feature.chat.presentation

import com.tolmol.app.core.ui.PixelButton as Button

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tolmol.app.core.common.LiveUpdate
import com.tolmol.app.core.ui.*
import com.tolmol.app.feature.chat.domain.*
import java.util.UUID
import kotlinx.coroutines.flow.first

class ConversationsViewModel(private val chat: ChatUseCase) : ScreenViewModel<List<Conversation>>() {
    val live = chat.live
    fun refresh() = load { chat.mine() }
    fun more() = load {
        val current = state.value.value.orEmpty()
        current + chat.mine(current.lastOrNull()?.id)
    }
}

data class ThreadState(val conversation: Conversation, val messages: List<Message>, val lastSent: String? = null)

class ChatViewModel(private val chat: ChatUseCase) : ScreenViewModel<ThreadState>() {
    val live = chat.live
    private var pending: Pair<String, String>? = null

    fun refresh(id: String) = load {
        val previous = state.value.value?.takeIf { it.conversation.id == id }
        val current = previous?.messages.orEmpty()
        val conversation = chat.thread(id)
        var after = current.lastOrNull()?.id
        val messages = current.toMutableList()
        do {
            val page = chat.messages(id, after)
            messages.addAll(page)
            after = page.lastOrNull()?.id
        } while (after != null && current.isNotEmpty() && page.size >= 50)
        ThreadState(conversation, messages.distinctBy { it.id }.sortedBy { it.id }, previous?.lastSent)
    }

    fun send(id: String, text: String) = load {
        val current = requireNotNull(state.value.value)
        val body = text.trim()
        val attempt = pending?.takeIf { it.first == body } ?: (body to UUID.randomUUID().toString()).also { pending = it }
        val message = chat.send(id, attempt.second, body)
        pending = null
        current.copy(messages = (current.messages + message).distinctBy { it.id }.sortedBy { it.id }, lastSent = body)
    }
}

class OpenChatViewModel(private val chat: ChatUseCase) : ScreenViewModel<Conversation>() {
    fun open(listing: String) = load { chat.open(listing) }
}

@Composable
fun ConversationsScreen(vm: ConversationsViewModel, user: String, listing: String?, open: (Conversation) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.refresh()
        vm.live.collect { if (it is LiveUpdate.Connected || it is LiveUpdate.Event && it.type == "chat.message") vm.refresh() }
    }
    val conversations = state.value?.filter { listing == null || it.listing == listing }
    Screen("Conversations") {
        ScreenFeedback(state) { vm.refresh() }
        if (conversations?.isEmpty() == true) Text("No conversations on this page.")
        for (thread in conversations.orEmpty()) {
            val other = if (user == thread.seller) "Buyer ${thread.buyer.take(8)}" else "Seller ${thread.seller.take(8)}"
            RowCard(other, "Lot ${thread.listing.take(8)}") { open(thread) }
        }
        if (state.value.orEmpty().size >= 25) TextButton({ vm.more() }, enabled = !state.loading) { Text("Load more") }
    }
}

@Composable
fun ChatThreadScreen(vm: ChatViewModel, id: String, user: String) {
    val state by vm.state.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(vm, id) {
        vm.refresh(id)
        vm.live.collect {
            val listing = vm.state.value.value?.conversation?.listing
            if (it is LiveUpdate.Connected || it is LiveUpdate.Event && (it.aggregate == id || it.aggregate == listing)) {
                vm.state.first { state -> !state.loading }
                vm.refresh(id)
            }
        }
    }
    LaunchedEffect(state.value?.messages?.lastOrNull()?.id, state.value?.lastSent) {
        if (draft.trim() == state.value?.lastSent) draft = ""
    }
    Screen("Conversation") {
        ScreenFeedback(state) { vm.refresh(id) }
        val thread = state.value
        if (thread?.messages?.isEmpty() == true) Text("No messages yet.")
        for (message in thread?.messages.orEmpty()) {
            Text(if (message.sender == user) "You" else "Other participant", style = MaterialTheme.typography.labelMedium)
            Text(message.body)
        }
        if (thread?.conversation?.writable == true) {
            OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth(), label = { Text("Message") })
            Button({ vm.send(id, draft) }, enabled = !state.loading && draft.isNotBlank() && draft.length <= 4000) { Text("Send") }
        } else if (thread != null) {
            Text("This conversation is read-only.")
        }
    }
}
