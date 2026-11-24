package com.tolmol.app.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

fun Throwable.serverMessage(): String = when (this) {
    is HttpException -> runCatching {
        Json.parseToJsonElement(response()?.errorBody()?.string().orEmpty())
            .jsonObject["error"]?.jsonPrimitive?.content
    }.getOrNull() ?: if (code() == 401) "Sign in again." else "Request failed (HTTP ${code()})."
    is IOException -> "Could not reach TolMol. Check your connection and retry."
    else -> message ?: "Something went wrong. Retry."
}
