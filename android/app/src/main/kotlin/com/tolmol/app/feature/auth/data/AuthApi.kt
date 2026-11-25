package com.tolmol.app.feature.auth.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable data class CredentialsDto(val email: String, val password: String)
@Serializable data class RegisterDto(val email: String, val password: String, val roles: Set<String>, val name: String? = null)
@Serializable data class GoogleLoginDto(val idToken: String)
@Serializable data class AccessTokenDto(val accessToken: String)
@Serializable data class AccountDto(val id: String, val roles: Set<String>)

interface AuthApi {
    @POST("api/auth/login") suspend fun login(@Body body: CredentialsDto): AccessTokenDto
    @POST("api/auth/register") suspend fun register(@Body body: RegisterDto)
    @POST("auth/google") suspend fun google(@Body body: GoogleLoginDto): AccessTokenDto
    @GET("api/me") suspend fun profile(@Header("Authorization") authorization: String): AccountDto
}
