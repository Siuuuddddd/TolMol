package com.tolmol.app.feature.listing.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

import kotlinx.serialization.json.JsonPrimitive

@Serializable data class ListingDto(val id: String, val category: String, val weight: JsonPrimitive, val startingRate: JsonPrimitive, val deadline: String, val status: String,val title: String? = null,val description: String? = null,val imageKey: String? = null)
@Serializable data class OfferDto(val id: String, val buyer: String, val rate: JsonPrimitive, val createdAt: String)
@Serializable data class AwardDto(val bidId: String)

interface ListingApi {
    @GET("api/me/listings") suspend fun mine(@Query("after") after: String?): List<ListingDto>
    @POST("api/listings") suspend fun publish(@Body body: JsonObject): ListingDto
    @GET("api/listings/{id}/bids") suspend fun offers(@Path("id") id: String): List<OfferDto>
    @POST("api/listings/{id}/accept") suspend fun accept(@Path("id") id: String, @Body body: AwardDto)
}
