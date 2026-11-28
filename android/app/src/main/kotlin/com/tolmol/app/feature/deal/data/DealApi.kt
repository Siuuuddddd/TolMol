package com.tolmol.app.feature.deal.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

import kotlinx.serialization.json.JsonPrimitive

@Serializable data class DealDto(val listing: String, val state: String, val buyer: String, val seller: String, val rate: JsonPrimitive,
    val estimatedWeight: JsonPrimitive, val actualWeight: JsonPrimitive? = null, val amount: JsonPrimitive? = null, val proposedBy: String? = null,
    val revision: Long, val relisted: String? = null, val pickupLatitude: Double, val pickupLongitude: Double,val coverPhotoUrl: String? = null,val photoUrls: List<String> = emptyList(),val imageKey: String? = null,val category: String = "OTHER")

interface DealApi {
    @GET("api/deals") suspend fun mine(): List<DealDto>
    @GET("api/deals/{id}") suspend fun deal(@Path("id") id: String): DealDto
    @POST("api/deals/{id}/inspect") suspend fun inspect(@Path("id") id: String, @Body body: JsonObject): DealDto
    @POST("api/deals/{id}/propose") suspend fun propose(@Path("id") id: String, @Body body: JsonObject): DealDto
    @POST("api/deals/{id}/confirm") suspend fun confirm(@Path("id") id: String, @Body body: JsonObject): DealDto
    @POST("api/deals/{id}/cancel") suspend fun cancel(@Path("id") id: String): DealDto
}
