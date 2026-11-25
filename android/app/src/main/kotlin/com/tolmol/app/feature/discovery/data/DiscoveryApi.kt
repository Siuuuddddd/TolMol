package com.tolmol.app.feature.discovery.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

@Serializable data class NearbyLotDto(val id: String, val category: String, val weight: kotlinx.serialization.json.JsonPrimitive, val startingRate: kotlinx.serialization.json.JsonPrimitive, val deadline: String, val status: String,val title: String? = null,val description: String? = null,val imageKey: String? = null,
    val distanceKm: Double? = null,val sellerDisplayName: String = "TolMol member",val sellerMemberSince: String? = null)

interface DiscoveryApi {
    @GET("api/listings/{id}") suspend fun detail(@Path("id") id: String): NearbyLotDto
    @GET("api/listings") suspend fun nearby(): List<NearbyLotDto>
    @GET("api/listings/watchlist") suspend fun watched(): List<NearbyLotDto>
    @PUT("api/listings/{id}/watch") suspend fun watch(@Path("id") id: String)
    @DELETE("api/listings/{id}/watch") suspend fun unwatch(@Path("id") id: String)
}
