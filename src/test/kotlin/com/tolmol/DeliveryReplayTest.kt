package com.tolmol
import com.tolmol.notification.domain.model.Alert
import com.tolmol.notification.service.*
import com.tolmol.platform.domain.model.LiveEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class DeliveryReplayTest : MarketFixture() {
    @Autowired lateinit var store: AlertStore
    @Test fun replayResumesStoredPendingAlert() {
        val user = UUID.randomUUID()
        val event = LiveEvent(UUID.randomUUID(),"BID_WON",UUID.randomUUID(),setOf(user),emptyMap())
        val alert = Alert(event.id,event.type,event.aggregate,event.data)
        assertTrue(store.pending(user,alert))
        var sent = 0
        val gateway = object : NotificationGateway {
            override fun send(user: UUID,alert: Alert): Boolean { sent++; return true }
            override fun broadcast(listing: UUID,excluded: Set<UUID>,allowed: (UUID)->Boolean,alert: Alert) {}
        }
        val push = object : PushProvider { override fun send(user: UUID,alert: Alert) = error("Unexpected push") }
        val service = NotificationService(store,gateway,push,market)
        repeat(2) { service.consume(event) }
        assertEquals(1,sent)
        assertEquals(1,store.inbox(user).size)
    }
}
