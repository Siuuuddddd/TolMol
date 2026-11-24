package com.tolmol.app.feature.notification.data
import com.tolmol.app.feature.notification.domain.*
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

class NotificationRepositoryImpl(private val api: NotificationApi, private val socket: LiveSocket) : NotificationRepository {
    override val live = socket.updates
    override suspend fun inbox(before: Long?) = request { api.inbox(before).map { Alert(it.eventId, it.type, it.aggregate, it.cursor, it.read) } }
    override suspend fun read(id: String) = request { api.read(id) }
    override suspend fun readAll() = request { api.readAll() }

    private suspend fun <T> request(action: suspend () -> T): T = try { action() }
    catch (error: CancellationException) { throw error }
    catch (error: Exception) { throw NotificationError.RequestFailed(error.serverMessage()) }
}

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    @Provides fun useCase(retrofit: Retrofit, socket: LiveSocket) =
        NotificationUseCase(NotificationRepositoryImpl(retrofit.create(NotificationApi::class.java), socket))
}
