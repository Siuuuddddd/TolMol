package com.tolmol.chat.api.controllers

import com.tolmol.chat.service.ChatService
import com.tolmol.chat.api.mappers.toDto
import org.springframework.web.bind.annotation.*
import java.util.UUID
import java.security.Principal

@RestController
@RequestMapping("/api")
class ChatController(private val chat: ChatService) {
    @PostMapping("/listings/{id}/chat")
    fun open(p: Principal,@PathVariable id: UUID) = chat.open(UUID.fromString(p.name),id)
    @GetMapping("/chat/{id}/messages")
    fun history(p: Principal,@PathVariable id: UUID,@RequestParam(required=false) after: Long?) =
        chat.history(UUID.fromString(p.name),id,after).map { it.toDto() }
}
