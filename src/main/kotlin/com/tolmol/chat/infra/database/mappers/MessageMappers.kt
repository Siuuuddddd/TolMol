package com.tolmol.chat.infra.database.mappers

import com.tolmol.chat.domain.model.Message
import com.tolmol.chat.infra.database.entities.MessageEntity
fun MessageEntity.toDomain() = Message(id,threadId,sender,body,sentAt)
