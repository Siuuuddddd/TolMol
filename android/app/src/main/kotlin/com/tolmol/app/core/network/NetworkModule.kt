package com.tolmol.app.core.network

import com.tolmol.app.BuildConfig
import com.tolmol.app.core.datastore.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import org.koin.dsl.module

val networkModule = module {
    single { Json { ignoreUnknownKeys = true } }

    single { createHttpClient(get()) }
    single { createRetrofit(get(), get()) }
}

private fun createHttpClient(session: SessionStore): OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val publicAuth = original.url.encodedPath.startsWith("/api/auth/") || original.url.encodedPath == "/auth/google"
            val token = if (publicAuth || original.header("Authorization") != null) null
                else runBlocking { session.read() }
            val request = if (token == null) original
                else original.newBuilder().header("Authorization", "Bearer $token").build()
            val response = chain.proceed(request)
            if (response.code == 401 && token != null) runBlocking { session.clear(token) }
            response
        }.build()

private fun createRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.REST_URL)
    .client(client)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
