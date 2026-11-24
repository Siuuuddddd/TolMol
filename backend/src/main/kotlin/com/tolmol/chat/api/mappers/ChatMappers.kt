package com.tolmol.chat.api.mappers

import com.tolmol.chat.api.dto.MessageDto
import com.tolmol.chat.domain.model.Message
fun Message.toDto() = MessageDto(id,threadId,sender,body,sentAt)
