package com.tolmol.app.core.network

import com.tolmol.app.BuildConfig
import com.tolmol.app.core.datastore.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun json() = Json { ignoreUnknownKeys = true }

    @Provides @Singleton
    fun client(session: SessionStore): OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val publicAuth = original.url.encodedPath.startsWith("/api/auth/")
            val token = if (publicAuth || original.header("Authorization") != null) null
                else runBlocking { session.read() }
            val request = if (token == null) original
                else original.newBuilder().header("Authorization", "Bearer $token").build()
            val response = chain.proceed(request)
            if (response.code == 401 && token != null) runBlocking { session.clear(token) }
            response
        }.build()

    @Provides @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.REST_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
