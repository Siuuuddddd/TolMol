package com.tolmol.app.feature.chat.data
import com.tolmol.app.feature.chat.domain.*
import com.tolmol.app.core.network.serverMessage
import com.tolmol.app.core.websocket.LiveSocket
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import retrofit2.Retrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.math.BigDecimal

class ChatRepositoryImpl(private val api: ChatApi, private val socket: LiveSocket) : ChatRepository {
    override val live = socket.updates
    override suspend fun mine(after: String?) = request { api.mine(after).map { it.toDomain() } }
    override suspend fun open(listing: String) = request { api.open(listing).toDomain() }
    override suspend fun thread(id: String) = request { api.thread(id).toDomain() }
    override suspend fun messages(id: String, after: Long?) = request { api.messages(id, after).map { it.toDomain() } }
    override suspend fun send(id: String, clientId: String, text: String) = request {
        socket.json.decodeFromJsonElement<MessageDto>(socket.command("chat.send", "threadId" to id, "clientId" to clientId, "body" to text)).toDomain()
    }

    private fun ConversationDto.toDomain() = Conversation(id, listing, buyer, seller, writable)
    private fun MessageDto.toDomain() = Message(id, threadId, sender, body, sentAt)

    private suspend fun <T> request(action: suspend () -> T): T = try { action() }
    catch (error: CancellationException) { throw error }
    catch (error: Exception) { throw ChatError.RequestFailed(error.serverMessage()) }
}

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {
    @Provides fun useCase(retrofit: Retrofit, socket: LiveSocket) =
        ChatUseCase(ChatRepositoryImpl(retrofit.create(ChatApi::class.java), socket))
}
