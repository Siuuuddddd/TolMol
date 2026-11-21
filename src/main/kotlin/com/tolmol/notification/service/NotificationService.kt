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
    fun inbox(user: UUID): List<Alert>
}
interface NotificationGateway {
    fun send(user: UUID,alert: Alert): Boolean
    fun broadcast(listing: UUID,excluded: Set<UUID>,allowed: (UUID)->Boolean,alert: Alert)
}
interface PushProvider { fun send(user: UUID,alert: Alert) }

@Service
class NotificationService(private val store: AlertStore,private val gateway: NotificationGateway,
    private val push: PushProvider,private val market: MarketplaceService) {
    fun inbox(user: UUID) = store.inbox(user)
    fun consume(event: LiveEvent) {
        val alert = Alert(event.id,event.type,event.aggregate,event.data)
        for (user in event.audience) {
            val inboxAlert = if (event.type=="chat.message") alert.copy(data=alert.data-"body") else alert
            if (store.pending(user,inboxAlert)) {
                if (!gateway.send(user,alert)) push.send(user,inboxAlert)
                store.delivered(user,event.id)
            }
        }
        if (event.type in setOf("BID_PLACED","LEADER_CHANGED","STATUS_CHANGED","AUCTION_AWARDED","AUCTION_EXPIRED")) {
            gateway.broadcast(event.aggregate,event.audience,{ user ->
                try { market.eligible(user,event.aggregate); true } catch (_: BusinessException) { false }
            },alert)
        }
    }
}
