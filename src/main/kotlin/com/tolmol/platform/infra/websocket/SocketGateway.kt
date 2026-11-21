package com.tolmol.platform.infra.websocket

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tolmol.platform.domain.exception.BusinessException
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface SocketCommand {
    val type: String
    fun handle(user: UUID,body: JsonNode): Any
}

@Component
class SocketGateway(private val json: ObjectMapper,commands: List<SocketCommand>) : TextWebSocketHandler() {
    private val handlers = commands.associateBy { it.type }
    private val sessions = ConcurrentHashMap<String,WebSocketSession>()
    override fun afterConnectionEstablished(session: WebSocketSession) {
        requireNotNull(session.principal)
        sessions[session.id] = ConcurrentWebSocketSessionDecorator(session,5000,65536)
    }
    override fun afterConnectionClosed(session: WebSocketSession,status: CloseStatus) { sessions.remove(session.id) }
    override fun handleTextMessage(session: WebSocketSession,message: TextMessage) {
        val response = try {
            val body = json.readTree(message.payload)
            val command = requireNotNull(handlers[body.path("type").asText()]) { "Unknown command" }
            mapOf("type" to "ack","data" to command.handle(UUID.fromString(session.principal!!.name),body))
        } catch (e: BusinessException) {
            mapOf("type" to "error","status" to e.status,"message" to e.message)
        } catch (e: IllegalArgumentException) {
            mapOf("type" to "error","status" to 400,"message" to "Invalid command")
        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            mapOf("type" to "error","status" to 400,"message" to "Invalid JSON")
        }
        sessions[session.id]?.sendMessage(TextMessage(json.writeValueAsString(response)))
    }
    fun deliver(user: UUID,payload: Any): Boolean {
        val message = TextMessage(json.writeValueAsString(payload))
        var sent = false
        for (session in sessions.values.filter { it.principal?.name==user.toString() }) {
            try {
                if (session.isOpen) { session.sendMessage(message); sent = true }
            } catch (_: java.io.IOException) { sessions.remove(session.id) }
            catch (_: IllegalStateException) { sessions.remove(session.id) }
        }
        return sent
    }
}
