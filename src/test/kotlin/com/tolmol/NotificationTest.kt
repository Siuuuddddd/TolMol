package com.tolmol
import com.fasterxml.jackson.databind.JsonNode
import com.tolmol.notification.service.NotificationService
import com.tolmol.user.domain.model.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.awaitility.Awaitility.await
import java.util.UUID
import java.util.concurrent.*

class NotificationTest : SocketFixture() {
    @Autowired lateinit var alerts: NotificationService
    @Test fun routesCommittedEventsOnlyToTheirAudiences() {
        val seller = user(Role.SELLER)
        val listing = lot(seller).id
        val watcher = user(Role.BUYER)
        val buyer = user(Role.BUYER)
        market.watch(watcher,listing,true)
        val inbox = LinkedBlockingQueue<JsonNode>()
        val socket = connect(seller,inbox)
        try {
            bid(buyer,listing,12)
            await().atMost(15,TimeUnit.SECONDS).until { alerts.inbox(watcher).any { it.type=="BID_PLACED" } }
            val thread = chat.open(buyer,listing)
            chat.send(buyer,thread.id,"Private pickup instructions",UUID.randomUUID())
            await().atMost(15,TimeUnit.SECONDS).until { inbox.any { it.path("type").asText()=="chat.message" } }
            assertTrue(inbox.any { it.path("type").asText()=="BID_PLACED" })
            assertFalse(alerts.inbox(watcher).any { it.type=="chat.message" })
            assertFalse(alerts.inbox(seller).first { it.type=="chat.message" }.data.containsKey("body"))
        } finally { socket.abort() }
    }
}
