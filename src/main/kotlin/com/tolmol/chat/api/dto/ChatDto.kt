package com.tolmol.chat.api.dto

import java.util.UUID
import java.time.Instant
data class SendMessageRequest(val threadId: UUID,val clientId: UUID,val body: String)
data class MessageDto(val id: Long,val threadId: UUID,val sender: UUID,val body: String,val sentAt: Instant)
