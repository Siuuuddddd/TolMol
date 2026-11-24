package com.tolmol.notification.service

import com.tolmol.notification.domain.model.Alert
import com.tolmol.platform.domain.model.LiveEvent
import com.tolmol.marketplace.service.MarketplaceService
import com.tolmol.platform.domain.exception.BusinessException
import org.springframework.stereotype.Service
import java.util.UUID

interface AlertStore {
    fun pending(user: UUID,alert: Alert): Boolean
    fun delivered(user: UUID,eventId: UUID)
    fun inbox(user: UUID,before: Long?=null,limit: Int=100): List<Alert>
    fun unread(user: UUID): Long
    fun markRead(user: UUID,id: UUID?)
    fun muteChat(user: UUID,muted: Boolean)
    fun muteListing(user: UUID,listing: UUID,muted: Boolean)
    fun muted(user: UUID,listing: UUID,chat: Boolean): Boolean
}
interface NotificationGateway {
    fun send(user: UUID,alert: Alert): Boolean
    fun broadcast(listing: UUID?,excluded: Set<UUID>,allowed: (UUID)->Boolean,alert: Alert)
}
interface PushProvider { fun send(user: UUID,alert: Alert) }

@Service
class NotificationService(private val store: AlertStore,private val gateway: NotificationGateway,
    private val push: PushProvider,private val market: MarketplaceService) {
    fun inbox(user: UUID,before: Long?=null,limit: Int=100): List<Alert> {
        require(limit in 1..100 && (before==null || before>0))
        return store.inbox(user,before,limit)
    }
    fun unread(user: UUID) = store.unread(user)
    fun markRead(user: UUID,id: UUID?) = store.markRead(user,id)
    fun muteChat(user: UUID,muted: Boolean) = store.muteChat(user,muted)
    fun muteListing(user: UUID,id: UUID,muted: Boolean) = store.muteListing(user,id,muted)
    fun consume(event: LiveEvent) {
        val alert = Alert(event.id,event.type,event.aggregate,event.data)
        val chat = event.type=="chat.message"
        val listing = if (chat) UUID.fromString(event.data.getValue("listing")) else event.aggregate
        for (user in event.audience) {
            val inboxAlert = if (event.type=="chat.message") alert.copy(data=alert.data-"body") else alert
            if (store.pending(user,inboxAlert)) {
                if (!store.muted(user,listing,chat) && !gateway.send(user,alert)) push.send(user,inboxAlert)
                store.delivered(user,event.id)
            }
        }
        val discovery = event.type in setOf("listing.available","listing.unavailable")
        if (discovery || event.type in setOf("BID_PLACED","LEADER_CHANGED","STATUS_CHANGED","AUCTION_AWARDED","AUCTION_EXPIRED")) {
            gateway.broadcast(if (discovery) null else event.aggregate,event.audience,{ user ->
                try { market.eligible(user,event.aggregate); !store.muted(user,listing,false) } catch (_: BusinessException) { false }
            },alert)
        }
    }
}
