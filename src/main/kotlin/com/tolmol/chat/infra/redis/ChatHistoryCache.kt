package com.tolmol.chat.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tolmol.chat.domain.model.Message
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.dao.DataAccessException
import java.util.UUID
import java.time.Duration

@Component
class ChatHistoryCache(private val redis: StringRedisTemplate,private val json: ObjectMapper) {
    fun recent(id: UUID,last: Long,load: ()->List<Message>): List<Message> {
        val key = "tolmol:chat:$id:$last"
        try { redis.opsForValue().get(key)?.let { return json.readValue<List<Message>>(it) } }
        catch (_: DataAccessException) { return load() }
        val messages = load()
        try { redis.opsForValue().set(key,json.writeValueAsString(messages),Duration.ofMinutes(10)) }
        catch (_: DataAccessException) { /* History is durable in Chat DB. */ }
        return messages
    }
}
