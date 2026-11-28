package com.tolmol.app.feature.listing.data
import com.tolmol.app.feature.listing.domain.*
import com.tolmol.app.core.network.serverMessage
import com.tolmol.app.core.websocket.LiveSocket
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.math.BigDecimal

class ListingRepositoryImpl(private val api: ListingApi) : ListingRepository {
    override suspend fun mine(after: String?) = request { api.mine(after).map { it.toDomain() } }
    override suspend fun publish(category: String, weight: BigDecimal, startingRate: BigDecimal, deadline: String,title: String?,description: String?) = request {
        api.publish(buildJsonObject {
            title?.let { put("title",it) }
            description?.let { put("description",it) }
            put("category", category)
            put("weight", JsonPrimitive(weight))
            put("startingRate", JsonPrimitive(startingRate))
            put("deadline", deadline)
        }).toDomain()
    }
    override suspend fun offers(id: String) = request { api.offers(id).map { Offer(it.id, it.buyer, it.rate.content.toBigDecimal(), it.createdAt) } }
    override suspend fun accept(id: String, bidId: String) = request { api.accept(id, AwardDto(bidId)) }

    private fun ListingDto.toDomain() = Listing(id, category, weight.content.toBigDecimal(), startingRate.content.toBigDecimal(), deadline, status,title,description,imageKey,coverPhotoUrl,photoUrls)

    private suspend fun <T> request(action: suspend () -> T): T = try { action() }
    catch (error: CancellationException) { throw error }
    catch (error: Exception) { throw ListingError.RequestFailed(error.serverMessage()) }
}
