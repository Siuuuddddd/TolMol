package com.tolmol.user.infra.database.repositories

import com.tolmol.platform.infra.config.Databases
import org.springframework.stereotype.Component
import org.springframework.scheduling.annotation.Scheduled
import java.sql.Timestamp
import java.util.UUID

@Component
class ActivityProjection(private val db: Databases) {
    data class Activity(val id: Long,val role: String,val userId: UUID,val listingId: UUID,val action: String,val occurredAt: Timestamp)

    @Scheduled(fixedDelayString = "\${tolmol.activity.delay-ms:2000}")
    fun project() {
        db.marketplace.transactions.executeWithoutResult {
            val events = db.marketplace.sql.sql("select * from role_activity_outbox order by id limit 100 for update skip locked")
                .query(Activity::class.java).list()
            for (event in events) {
                val schema = when (event.role) { "buyer" -> "buyer"; "seller" -> "seller"; else -> error("Unknown role") }
                db.users.sql.sql("""insert into $schema.activity(event_id,user_id,listing_id,action,occurred_at)
                    values(?,?,?,?,?) on conflict(event_id) do nothing""")
                    .params(event.id,event.userId,event.listingId,event.action,event.occurredAt).update()
                db.marketplace.sql.sql("delete from role_activity_outbox where id=?").param(event.id).update()
            }
        }
    }
}
