package com.tolmol.app.feature.bidding.domain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import com.tolmol.app.core.common.LiveUpdate

data class Auction(val listing: String, val highest: BigDecimal?, val leader: String?, val status: String, val version: Long)


sealed class BiddingError(message: String) : RuntimeException(message) {
    class RequestFailed(message: String) : BiddingError(message)
}

interface BiddingRepository {
    val live: Flow<LiveUpdate>
    suspend fun auction(id: String): Auction
    suspend fun place(id: String, rate: BigDecimal, key: String)

    suspend fun subscribe(id: String): Auction
    suspend fun unsubscribe(id: String)
}

class BiddingUseCase(repository: BiddingRepository) : BiddingRepository by repository
