package com.tolmol
import com.fasterxml.jackson.databind.*
import com.tolmol.chat.domain.exception.ConversationUnavailableException
import com.tolmol.chat.service.ChatService
import com.tolmol.platform.infra.kafka.EventOutbox
import com.tolmol.user.domain.model.Role
import com.tolmol.user.service.TokenIssuer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.*
import java.util.UUID
import java.util.concurrent.*

abstract class SocketFixture : AuctionFixture() {
    @LocalServerPort var port = 0
    @Autowired lateinit var issuer: TokenIssuer
    @Autowired lateinit var json: ObjectMapper
    @Autowired lateinit var chat: ChatService
    @Autowired lateinit var relay: EventOutbox
    fun connect(user: UUID,inbox: BlockingQueue<JsonNode>): WebSocket {
        return HttpClient.newHttpClient().newWebSocketBuilder().header("Authorization","Bearer "+issuer.issue(user))
            .buildAsync(URI("ws://localhost:$port/ws"),object : WebSocket.Listener {
                val buffer = StringBuilder()
                override fun onText(ws: WebSocket,data: CharSequence,last: Boolean): CompletionStage<*>? {
                    buffer.append(data)
                    if (last) { inbox.add(json.readTree(buffer.toString())); buffer.setLength(0) }
                    ws.request(1)
                    return null
                }
            }).join()
    }
}
class ChatTest : SocketFixture() {
    @Test fun authenticatedSocketPersistsPrivateMessages() {
        val seller = user(Role.SELLER)
        val buyer = user(Role.BUYER)
        val thread = chat.open(buyer,lot(seller).id)
        assertTrue(chat.details(seller, thread.id).second)
        assertThrows(ConversationUnavailableException::class.java) { chat.details(UUID.randomUUID(), thread.id) }
        val inbox = LinkedBlockingQueue<JsonNode>()
        val socket = connect(buyer,inbox)
        try {
            val command = json.writeValueAsString(mapOf("type" to "chat.send","threadId" to thread.id,"clientId" to UUID.randomUUID(),"body" to "Pickup today?"))
            socket.sendText(command,true).join()
            assertEquals("ack",inbox.poll(8,TimeUnit.SECONDS)?.path("type")?.asText())
            val message = chat.history(seller,thread.id,null).single()
            assertEquals("Pickup today?",message.body)
            assertEquals(message,chat.history(buyer,thread.id,0).single())
            assertThrows(ConversationUnavailableException::class.java) { chat.history(UUID.randomUUID(),thread.id,null) }
            relay.publishChat()
        } finally { socket.abort() }
    }
}
