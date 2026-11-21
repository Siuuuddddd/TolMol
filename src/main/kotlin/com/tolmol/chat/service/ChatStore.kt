package com.tolmol.chat.service

import com.tolmol.chat.domain.model.*
import java.util.UUID
interface ChatStore {
    fun open(listing: UUID,seller: UUID,buyer: UUID): Conversation
    fun thread(id: UUID): Conversation
    fun send(thread: Conversation,sender: UUID,body: String,clientId: UUID): Message
    fun history(id: UUID,after: Long?): List<Message>
}
