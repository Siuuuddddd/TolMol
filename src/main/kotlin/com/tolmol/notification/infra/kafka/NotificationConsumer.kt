package com.tolmol.notification.infra.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.tolmol.notification.service.NotificationService
import com.tolmol.platform.domain.model.LiveEvent
import org.springframework.stereotype.Component
import org.springframework.kafka.annotation.KafkaListener
@Component
class NotificationConsumer(private val notifications: NotificationService,private val json: ObjectMapper) {
    @KafkaListener(topics=["auction.events","chat.message"], groupId="tolmol-notifications")
    fun receive(value: String) = notifications.consume(json.readValue(value,LiveEvent::class.java))
}
