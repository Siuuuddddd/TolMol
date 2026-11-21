package com.tolmol.chat.api.controllers

import com.tolmol.chat.service.ChatService
import com.tolmol.chat.domain.model.Conversation
import org.springframework.web.bind.annotation.*
import java.util.UUID
import java.security.Principal

@RestController
class MyChatController(private val service: ChatService) {
    @GetMapping("/api/me/conversations")
    fun mine(p: Principal,@RequestParam(required=false) after: UUID?,@RequestParam(defaultValue="25") limit: Int) =
        service.mine(UUID.fromString(p.name),after,limit).map { it }
}
