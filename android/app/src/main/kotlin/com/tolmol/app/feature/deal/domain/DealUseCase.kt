package com.tolmol.app.feature.deal.domain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import com.tolmol.app.core.common.LiveUpdate

enum class DealState { PENDING_PICKUP, WEIGHT_DISPUTE, COMPLETED, CANCELLED_RELISTED }
data class Deal(val listing: String, val state: DealState, val buyer: String, val seller: String, val rate: BigDecimal,
    val estimatedWeight: BigDecimal, val actualWeight: BigDecimal?, val amount: BigDecimal?, val proposedBy: String?,
    val revision: Long, val relisted: String?, val pickupLatitude: Double, val pickupLongitude: Double)

sealed class DealError(message: String) : RuntimeException(message) {
    class RequestFailed(message: String) : DealError(message)
}

interface DealRepository {
    val live: Flow<LiveUpdate>
    suspend fun mine(): List<Deal>
    suspend fun deal(id: String): Deal
    suspend fun inspect(id: String, weight: BigDecimal): Deal
    suspend fun propose(id: String, amount: BigDecimal): Deal
    suspend fun confirm(id: String, revision: Long): Deal
    suspend fun cancel(id: String): Deal
}

class DealUseCase(repository: DealRepository) : DealRepository by repository
