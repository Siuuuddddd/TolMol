package com.tolmol
import com.tolmol.notification.domain.model.Alert
import com.tolmol.notification.service.*
import com.tolmol.platform.domain.model.LiveEvent
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class InboxTest : MarketFixture() {
    @Autowired lateinit var store: AlertStore
    @Test fun scopedInboxAndPreferences() {
        val owner = UUID.randomUUID()
        val stranger = UUID.randomUUID()
        val listing = UUID.randomUUID()
        var socketAttempts = 0
        var pushes = 0
        val gateway = object : NotificationGateway {
            override fun send(user: UUID,alert: Alert): Boolean { socketAttempts++; return false }
            override fun broadcast(listing: UUID?,excluded: Set<UUID>,allowed: (UUID)->Boolean,alert: Alert) {}
        }
        val push = object : PushProvider { override fun send(user: UUID,alert: Alert) { pushes++ } }
        val service = NotificationService(store,gateway,push,market)
        fun emit(type: String) = service.consume(LiveEvent(UUID.randomUUID(),type,listing,setOf(owner),mapOf("listing" to listing.toString())))
        service.muteChat(owner,true)
        emit("chat.message")
        service.muteListing(owner,listing,true)
        emit("BID_WON")
        assertEquals(0,socketAttempts)
        assertEquals(0,pushes)
        service.muteListing(owner,listing,false)
        emit("BID_WON")
        assertEquals(1,socketAttempts)
        assertEquals(1,pushes)
        val page = service.inbox(owner,null,1).single()
        assertEquals(2,service.inbox(owner,page.cursor,5).size)
        service.markRead(stranger,page.eventId)
        service.markRead(stranger,null)
        assertTrue(service.inbox(stranger).isEmpty())
        assertEquals(3L,service.unread(owner))
        service.markRead(owner,page.eventId)
        assertTrue(service.inbox(owner,null,1).single().read)
        service.markRead(owner,null)
        assertEquals(0L,service.unread(owner))
    }
}
