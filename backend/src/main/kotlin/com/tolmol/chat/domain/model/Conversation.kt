package com.tolmol.chat.domain.model

import java.util.UUID
import java.time.Instant
data class Conversation(val id: UUID,val listing: UUID,val seller: UUID,val buyer: UUID)
data class Message(val id: Long,val threadId: UUID,val sender: UUID,val body: String,val sentAt: Instant)
