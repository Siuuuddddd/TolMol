package com.tolmol.app.core.websocket

import com.tolmol.app.BuildConfig
import com.tolmol.app.core.common.LiveUpdate
import com.tolmol.app.core.datastore.SessionStore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSocket @Inject constructor(
    private val client: OkHttpClient,
    private val session: SessionStore,
    val json: Json,
) {
    private class Connection(val socket: WebSocket, val replies: Channel<JsonObject>)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connection = MutableStateFlow<Connection?>(null)
    private val events = MutableSharedFlow<LiveUpdate.Event>(extraBufferCapacity = 64)
    private val commandLock = Mutex()
    val updates: Flow<LiveUpdate> = merge(events, connection.map {
        if (it == null) LiveUpdate.Disconnected else LiveUpdate.Connected
    })

    init {
        scope.launch {
            session.current.collectLatest { token ->
                if (token == null) return@collectLatest
                var delayMs = 500L
                while (currentCoroutineContext().isActive) {
                    if (connect(token)) delayMs = 500L
                    delay(delayMs)
                    delayMs = (delayMs * 2).coerceAtMost(8_000L)
                }
            }
        }
    }

    suspend fun command(type: String, vararg fields: Pair<String, String>): JsonElement = commandLock.withLock {
        val current = connection.value ?: throw IOException("Connecting to live updates. Retry in a moment.")
        val payload = buildJsonObject {
            put("type", type)
            for ((key, value) in fields) put(key, value)
        }
        if (!current.socket.send(payload.toString())) throw IOException("Live connection closed. Retry.")
        val response = try {
            withTimeoutOrNull(10_000) { current.replies.receiveCatching().getOrNull() }
        } catch (cancelled: CancellationException) {
            current.socket.cancel()
            throw cancelled
        } ?: run {
            current.socket.cancel()
            throw IOException("Live request timed out. Retry.")
        }
        if (response["type"]?.jsonPrimitive?.content == "error") {
            throw IllegalStateException(response["message"]?.jsonPrimitive?.content ?: "Live request failed.")
        }
        response["data"] ?: JsonNull
    }

    private suspend fun connect(token: String): Boolean {
        val opened = CompletableDeferred<WebSocket>()
        val closed = CompletableDeferred<Unit>()
        val replies = Channel<JsonObject>(Channel.UNLIMITED)
        val seen = LinkedHashSet<String>()
        val request = Request.Builder().url(BuildConfig.SOCKET_URL).header("Authorization", "Bearer $token").build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { opened.complete(webSocket) }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val frame = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
                val type = (frame["type"] as? JsonPrimitive)?.content ?: return
                if (type == "ack" || type == "error") {
                    replies.trySend(frame)
                    return
                }
                val id = (frame["eventId"] as? JsonPrimitive)?.content ?: return
                val aggregate = (frame["aggregate"] as? JsonPrimitive)?.content ?: return
                if (!seen.add(id)) return
                if (seen.size > 256) seen.remove(seen.first())
                val data = (frame["data"] as? JsonObject).orEmpty().mapNotNull { (key, item) ->
                    (item as? JsonPrimitive)?.let { key to it.content }
                }.toMap()
                events.tryEmit(LiveUpdate.Event(id, type, aggregate, data))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (code == 1008) scope.launch { session.clear(token) }
                closed.complete(Unit)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (response?.code == 401) scope.launch { session.clear(token) }
                closed.complete(Unit)
            }
        })
        return try {
            val ready = withTimeoutOrNull(12_000) {
                kotlinx.coroutines.selects.select<WebSocket?> {
                    opened.onAwait { it }
                    closed.onAwait { null }
                }
            } ?: return false
            connection.value = Connection(ready, replies)
            closed.await()
            true
        } finally {
            connection.value = null
            replies.close()
            socket.cancel()
        }
    }
}
