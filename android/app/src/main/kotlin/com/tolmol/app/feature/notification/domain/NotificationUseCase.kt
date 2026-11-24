package com.tolmol.app.feature.notification.domain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import com.tolmol.app.core.common.LiveUpdate

data class Alert(val eventId: String, val type: String, val aggregate: String, val cursor: Long?, val read: Boolean)

sealed class NotificationError(message: String) : RuntimeException(message) {
    class RequestFailed(message: String) : NotificationError(message)
}

interface NotificationRepository {
    val live: Flow<LiveUpdate>
    suspend fun inbox(before: Long? = null): List<Alert>
    suspend fun read(id: String)
    suspend fun readAll()
}

class NotificationUseCase(repository: NotificationRepository) : NotificationRepository by repository
