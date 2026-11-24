package com.tolmol.app.feature.chat.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

@Serializable data class ConversationDto(val id: String, val listing: String, val buyer: String, val seller: String, val writable: Boolean = false)
@Serializable data class MessageDto(val id: Long, val threadId: String, val sender: String, val body: String, val sentAt: String)

interface ChatApi {
    @GET("api/me/conversations") suspend fun mine(@Query("after") after: String?): List<ConversationDto>
    @POST("api/listings/{id}/chat") suspend fun open(@Path("id") listing: String): ConversationDto
    @GET("api/chat/{id}") suspend fun thread(@Path("id") id: String): ConversationDto
    @GET("api/chat/{id}/messages") suspend fun messages(@Path("id") id: String, @Query("after") after: Long?): List<MessageDto>
}
