package com.tolmol.app.feature.bidding.data
import com.tolmol.app.feature.bidding.domain.*
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

class BiddingRepositoryImpl(private val api: BiddingApi, private val socket: LiveSocket) : BiddingRepository {
    override val live = socket.updates
    override suspend fun auction(id: String) = request { api.auction(id).toDomain() }
    override suspend fun place(id: String, rate: BigDecimal, key: String) = request {
        api.place(id, buildJsonObject {
            put("rate", JsonPrimitive(rate))
            put("requestKey", key)
        })
    }
    override suspend fun mine(after: String?) = request {
        api.mine(after).map { MyBid(it.id, it.listing, it.rate.content.toBigDecimal(), it.createdAt, it.status) }
    }
    override suspend fun subscribe(id: String) = request {
        socket.json.decodeFromJsonElement<AuctionDto>(socket.command("auction.subscribe", "listingId" to id)).toDomain()
    }
    override suspend fun unsubscribe(id: String) { request { socket.command("auction.unsubscribe", "listingId" to id) } }

    private fun AuctionDto.toDomain() = Auction(listing, highest?.content?.toBigDecimal(), leader, status, version)

    private suspend fun <T> request(action: suspend () -> T): T = try { action() }
    catch (error: CancellationException) { throw error }
    catch (error: Exception) { throw BiddingError.RequestFailed(error.serverMessage()) }
}

@Module
@InstallIn(SingletonComponent::class)
object BiddingModule {
    @Provides fun useCase(retrofit: Retrofit, socket: LiveSocket) =
        BiddingUseCase(BiddingRepositoryImpl(retrofit.create(BiddingApi::class.java), socket))
}
