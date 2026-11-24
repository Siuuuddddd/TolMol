package com.tolmol.chat.infra.database.entities

import java.util.UUID
import java.time.Instant
data class MessageEntity(val id: Long,val threadId: UUID,val sender: UUID,val body: String,val sentAt: Instant)
