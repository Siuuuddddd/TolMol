package com.tolmol.app.feature.bidding.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

import kotlinx.serialization.json.JsonPrimitive

@Serializable data class AuctionDto(val listing: String, val highest: JsonPrimitive? = null, val leader: String? = null, val status: String, val version: Long)
@Serializable data class BidDto(val id: String, val listing: String, val rate: JsonPrimitive, val createdAt: String, val status: String)

interface BiddingApi {
    @GET("api/listings/{id}/auction") suspend fun auction(@Path("id") id: String): AuctionDto
    @POST("api/listings/{id}/bids") suspend fun place(@Path("id") id: String, @Body body: JsonObject)
    @GET("api/me/bids") suspend fun mine(@Query("after") after: String?): List<BidDto>
}
