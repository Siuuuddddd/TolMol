package com.tolmol.notification.infra.websocket

import com.tolmol.notification.domain.model.Alert
import com.tolmol.notification.service.NotificationGateway
import com.tolmol.platform.infra.websocket.SocketGateway
import org.springframework.stereotype.Component
import java.util.UUID
@Component
class AlertGateway(private val sockets: SocketGateway) : NotificationGateway {
    override fun broadcast(listing: UUID,excluded: Set<UUID>,allowed: (UUID)->Boolean,alert: Alert) = sockets.broadcast(listing,excluded,allowed,alert)
    override fun send(user: UUID,alert: Alert) = sockets.deliver(user,alert)
}
