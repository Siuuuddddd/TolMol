package com.tolmol.platform.infra.websocket

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tolmol.platform.domain.exception.BusinessException
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import com.tolmol.platform.service.SocketCommand

@Component
class SocketGateway(private val json: ObjectMapper,commands: List<SocketCommand>) : TextWebSocketHandler() {
    private val timer = java.util.concurrent.ScheduledThreadPoolExecutor(1).apply { removeOnCancelPolicy = true }
    private val expiryTasks = ConcurrentHashMap<String,java.util.concurrent.ScheduledFuture<*>>()
    @jakarta.annotation.PreDestroy fun shutdown() { timer.shutdownNow() }
    private fun expires(session: WebSocketSession) =
        (session.principal as org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken).token.expiresAt!!
    private val handlers = commands.associateBy { it.type }
    private val sessions = ConcurrentHashMap<String,WebSocketSession>()
    private val subscriptions = ConcurrentHashMap<String,MutableSet<UUID>>()
    fun broadcast(listing: UUID,excluded: Set<UUID>,allowed: (UUID)->Boolean,payload: Any) {
        for (socket in sessions.values) synchronized(socket) {
            val user = UUID.fromString(socket.principal!!.name)
            if (user !in excluded && subscriptions[socket.id]?.contains(listing)==true && allowed(user)) send(socket,payload)
        }
    }
    private fun send(socket: WebSocketSession,payload: Any): Boolean {
        if (!socket.isOpen || !expires(socket).isAfter(java.time.Instant.now())) return false
        return try { socket.sendMessage(TextMessage(json.writeValueAsString(payload))); true }
        catch (_: java.io.IOException) { false } catch (_: IllegalStateException) { false }
    }
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val delay = java.time.Duration.between(java.time.Instant.now(),expires(session)).toMillis().coerceAtLeast(0)
        expiryTasks[session.id] = timer.schedule({ session.close(CloseStatus.POLICY_VIOLATION) },delay,java.util.concurrent.TimeUnit.MILLISECONDS)
        sessions[session.id] = ConcurrentWebSocketSessionDecorator(session,5000,65536)
    }
    override fun afterConnectionClosed(session: WebSocketSession,status: CloseStatus) { sessions.remove(session.id); subscriptions.remove(session.id); expiryTasks.remove(session.id)?.cancel(false) }
    override fun handleTextMessage(session: WebSocketSession,message: TextMessage) {
        val socket = sessions[session.id] ?: return
        synchronized(socket) {
        if (!expires(socket).isAfter(java.time.Instant.now())) { socket.close(CloseStatus.POLICY_VIOLATION); return }
        val response = try {
            val body = json.readTree(message.payload)
            if (body.path("type").asText()=="auction.unsubscribe") {
                subscriptions[session.id]?.remove(UUID.fromString(body.path("listingId").asText()))
                send(socket,mapOf("type" to "ack","data" to "unsubscribed"))
                return
            }
            val command = requireNotNull(handlers[body.path("type").asText()]) { "Unknown command" }
            val result = command.handle(UUID.fromString(session.principal!!.name),body)
            if (command.type=="auction.subscribe") subscriptions.computeIfAbsent(session.id) { ConcurrentHashMap.newKeySet() }
                .add(UUID.fromString(body.path("listingId").asText()))
            mapOf("type" to "ack","data" to result)
        } catch (e: BusinessException) {
            mapOf("type" to "error","status" to e.status,"message" to e.message)
        } catch (e: IllegalArgumentException) {
            mapOf("type" to "error","status" to 400,"message" to "Invalid command")
        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            mapOf("type" to "error","status" to 400,"message" to "Invalid JSON")
        }
        send(socket,response)
        }
    }
    fun deliver(user: UUID,payload: Any): Boolean {

        var sent = false
        for (session in sessions.values.filter { it.principal?.name==user.toString() }) {
            synchronized(session) { if (send(session,payload)) sent = true }
        }
        return sent
    }
}
