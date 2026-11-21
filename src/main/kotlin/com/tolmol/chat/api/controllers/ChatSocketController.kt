package com.tolmol.chat.api.controllers

import com.fasterxml.jackson.databind.JsonNode
import com.tolmol.platform.infra.websocket.SocketCommand
import com.tolmol.chat.service.ChatService
import com.tolmol.chat.api.dto.SendMessageRequest
import com.tolmol.chat.api.mappers.toDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ChatSocketController(private val chat: ChatService) : SocketCommand {
    override val type = "chat.send"
    override fun handle(user: UUID,body: JsonNode): Any {
        val req = SendMessageRequest(UUID.fromString(body.path("threadId").asText()),UUID.fromString(body.path("clientId").asText()),body.path("body").asText(""))
        return chat.send(user,req.threadId,req.body,req.clientId).toDto()
    }
}
