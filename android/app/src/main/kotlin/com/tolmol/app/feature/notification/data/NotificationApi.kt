package com.tolmol.app.feature.notification.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

@Serializable data class AlertDto(val eventId: String, val type: String, val aggregate: String, val cursor: Long? = null, val read: Boolean)

interface NotificationApi {
    @GET("api/notifications") suspend fun inbox(@Query("before") before: Long?): List<AlertDto>
    @PUT("api/notifications/{id}/read") suspend fun read(@Path("id") id: String)
    @PUT("api/notifications/read-all") suspend fun readAll()
}
