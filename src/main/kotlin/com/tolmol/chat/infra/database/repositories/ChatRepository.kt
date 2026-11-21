package com.tolmol.chat.infra.database.repositories

import com.tolmol.chat.domain.model.*
import com.tolmol.chat.domain.exception.*
import com.tolmol.chat.service.ChatStore
import com.tolmol.chat.infra.database.entities.MessageEntity
import com.tolmol.chat.infra.database.mappers.toDomain
import com.tolmol.chat.infra.redis.ChatHistoryCache
import com.tolmol.platform.infra.config.Databases
import com.tolmol.platform.infra.kafka.EventOutbox
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ChatRepository(dbs: Databases,private val cache: ChatHistoryCache,private val events: EventOutbox) : ChatStore {
    private val db = dbs.chat
    private val sql = db.sql
    override fun open(listing: UUID,seller: UUID,buyer: UUID) = sql.sql("""insert into threads values(?,?,?,?) on conflict(listing,buyer)
        do update set buyer=excluded.buyer returning *""").params(UUID.randomUUID(),listing,seller,buyer).query(Conversation::class.java).single()
    override fun thread(id: UUID) = sql.sql("select * from threads where id=?").param(id).query(Conversation::class.java)
        .optional().orElse(null) ?: throw ConversationUnavailableException()
    override fun send(thread: Conversation,sender: UUID,body: String,clientId: UUID): Message = db.transactions.execute {
        sql.sql("select id from threads where id=? for update").param(thread.id).query(UUID::class.java).single()
        val existing = sql.sql("select * from messages where thread_id=? and sender=? and client_id=?").params(thread.id,sender,clientId)
            .query(MessageEntity::class.java).optional().orElse(null)
        if (existing!=null) {
            require(existing.body==body) { "Message key already used" }
            return@execute existing.toDomain()
        }
        val message = sql.sql("insert into messages(thread_id,sender,body,client_id) values(?,?,?,?) returning *")
            .params(thread.id,sender,body,clientId).query(MessageEntity::class.java).single().toDomain()
        val recipient = if (sender==thread.seller) thread.buyer else thread.seller
        events.append(db,"chat.message",thread.id,setOf(recipient),mapOf("listing" to thread.listing.toString(),
            "sender" to sender.toString(),"messageId" to message.id.toString(),"body" to body))
        message
    }!!
    override fun history(id: UUID,after: Long?): List<Message> {
        if (after!=null) return sql.sql("select * from messages where thread_id=? and id>? order by id limit 50")
            .params(id,after).query(MessageEntity::class.java).list().map { it.toDomain() }
        val last = sql.sql("select coalesce(max(id),0) from messages where thread_id=?").param(id).query(Long::class.java).single()
        return cache.recent(id,last) {
            sql.sql("select * from messages where thread_id=? and id<=? order by id desc limit 50").params(id,last)
                .query(MessageEntity::class.java).list().asReversed().map { it.toDomain() }
        }
    }
}
