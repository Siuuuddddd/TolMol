package com.tolmol.notification.api.controllers

import com.tolmol.notification.service.NotificationService
import com.tolmol.notification.api.mappers.toDto
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.UUID
@RestController
class NotificationController(private val service: NotificationService) {
    @GetMapping("/api/notifications") fun inbox(p: Principal) = service.inbox(UUID.fromString(p.name)).map { it.toDto() }
}
