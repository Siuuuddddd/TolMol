package com.tolmol.app.feature.chat.domain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import com.tolmol.app.core.common.LiveUpdate

data class Conversation(val id: String, val listing: String, val buyer: String, val seller: String, val writable: Boolean)
data class Message(val id: Long, val threadId: String, val sender: String, val body: String, val sentAt: String)

sealed class ChatError(message: String) : RuntimeException(message) {
    class RequestFailed(message: String) : ChatError(message)
}

interface ChatRepository {
    val live: Flow<LiveUpdate>
    suspend fun mine(after: String? = null): List<Conversation>
    suspend fun open(listing: String): Conversation
    suspend fun thread(id: String): Conversation
    suspend fun messages(id: String, after: Long? = null): List<Message>
    suspend fun send(id: String, clientId: String, text: String): Message
}

class ChatUseCase(repository: ChatRepository) : ChatRepository by repository
