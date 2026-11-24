package com.tolmol.notification.infra.config

import com.tolmol.notification.domain.model.Alert
import com.tolmol.notification.service.PushProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
@Component
class LoggingPushProvider : PushProvider {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun send(user: UUID,alert: Alert) { log.info("Push stub recipient={} event={} type={}",user,alert.eventId,alert.type) }
}
