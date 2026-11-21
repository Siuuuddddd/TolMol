package com.tolmol.platform.infra.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.tolmol.platform.domain.model.LiveEvent
import com.tolmol.platform.infra.config.*
import org.springframework.stereotype.Component
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class EventOutbox(private val db: Databases,private val json: ObjectMapper,private val kafka: KafkaTemplate<String,String>) {
    fun append(database: Database,type: String,aggregate: UUID,audience: Set<UUID>,data: Map<String,String>) {
        val event = LiveEvent(UUID.randomUUID(),type,aggregate,audience,data)
        database.sql.sql("insert into outbox(id,aggregate_id,event_type,payload) values(?,?,?,?)")
            .params(event.id,aggregate,type,json.writeValueAsString(event)).update()
    }
    @Scheduled(fixedDelayString = "\${tolmol.outbox.delay-ms:500}")
    fun publishMarketplace() = publish(db.marketplace,"auction.events")
    @Scheduled(fixedDelayString = "\${tolmol.outbox.delay-ms:500}")
    fun publishChat() = publish(db.chat,"chat.message")

    data class Pending(val id: UUID,val aggregateId: UUID,val payload: String)
    private fun publish(database: Database,topic: String) {
        database.transactions.executeWithoutResult {
            val pending = database.sql.sql("select id,aggregate_id,payload from outbox where not published order by seq limit 50 for update")
                .query(Pending::class.java).list()
            for (event in pending) {
                kafka.send(topic,event.aggregateId.toString(),event.payload).get(8,TimeUnit.SECONDS)
                database.sql.sql("update outbox set published=true where id=?").param(event.id).update()
            }
        }
    }
}
