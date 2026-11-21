package com.tolmol.notification.api.controllers

import com.tolmol.notification.service.NotificationService
import com.tolmol.notification.api.mappers.toDto
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.UUID
@RestController
class NotificationController(private val service: NotificationService) {
    @GetMapping("/api/notifications") fun inbox(p: Principal,@RequestParam(required=false) before: Long?,@RequestParam(defaultValue="100") limit: Int) =
        service.inbox(UUID.fromString(p.name),before,limit).map { it.toDto() }
    @GetMapping("/api/notifications/unread-count") fun unread(p: Principal) = mapOf("count" to service.unread(UUID.fromString(p.name)))
    @PutMapping("/api/notifications/{id}/read") fun read(p: Principal,@PathVariable id: UUID) = service.markRead(UUID.fromString(p.name),id)
    @PutMapping("/api/notifications/read-all") fun readAll(p: Principal) = service.markRead(UUID.fromString(p.name),null)
    @PutMapping("/api/notifications/preferences/chat") fun chat(p: Principal,@RequestBody req: com.tolmol.notification.api.dto.ChatPreferenceRequest) =
        service.muteChat(UUID.fromString(p.name),req.muted)
    @PutMapping("/api/notifications/preferences/listings/{id}") fun mute(p: Principal,@PathVariable id: UUID) = service.muteListing(UUID.fromString(p.name),id,true)
    @DeleteMapping("/api/notifications/preferences/listings/{id}") fun unmute(p: Principal,@PathVariable id: UUID) = service.muteListing(UUID.fromString(p.name),id,false)
}
