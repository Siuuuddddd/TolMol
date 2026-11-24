package com.tolmol.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
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
    val current = tokens.stateIn(CoroutineScope(SupervisorJob() + Dispatchers.IO), SharingStarted.Eagerly, null)

    suspend fun read() = tokens.first()
    suspend fun save(token: String) { store.edit { it[key] = token } }
    suspend fun clear(expected: String? = null) {
        store.edit { if (expected == null || it[key] == expected) it.remove(key) }
    }
}
