package com.tolmol.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.Duration
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore("session")

@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.sessionDataStore
    private val key = stringPreferencesKey("jwt")
    private val tokens = store.data.map { it[key] }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val current = tokens.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch {
            current.collectLatest { token ->
                if (token != null) {
                    val expires = expiry(token) ?: Instant.now()
                    delay(Duration.between(Instant.now(), expires).toMillis().coerceAtLeast(0))
                    clear(token)
                }
            }
        }
    }

    suspend fun read() = tokens.first()
    suspend fun save(token: String) { store.edit { it[key] = token } }
    suspend fun clear(expected: String? = null) {
        store.edit { if (expected == null || it[key] == expected) it.remove(key) }
    }
}

internal fun expiry(token: String): Instant? = runCatching {
    val payload = Base64.getUrlDecoder().decode(token.split(".")[1]).decodeToString()
    Instant.ofEpochSecond(Json.parseToJsonElement(payload).jsonObject.getValue("exp").jsonPrimitive.long)
}.getOrNull()
