package com.tolmol.notification.infra.database.repositories

import com.tolmol.notification.domain.model.Alert
import com.tolmol.notification.service.AlertStore
import com.tolmol.notification.infra.database.entities.AlertEntity
import com.tolmol.notification.infra.database.mappers.toDomain
import com.tolmol.platform.infra.config.Databases
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Repository
import java.util.UUID
@Repository
class NotificationRepository(dbs: Databases,private val json: ObjectMapper) : AlertStore {
    private val sql = dbs.marketplace.sql
    override fun pending(user: UUID,alert: Alert) = sql.sql("""insert into alerts(event_id,recipient,kind,aggregate_id,data)
        values(?,?,?,?,?) on conflict(recipient,event_id) do update set event_id=excluded.event_id returning not delivered""")
        .params(alert.eventId,user,alert.type,alert.aggregate,json.writeValueAsString(alert.data)).query(Boolean::class.java).single()
    override fun delivered(user: UUID,eventId: UUID) {
        sql.sql("update alerts set delivered=true where recipient=? and event_id=?").params(user,eventId).update()
    }
    override fun inbox(user: UUID) = sql.sql("select * from alerts where recipient=? order by sequence desc limit 100")
        .param(user).query(AlertEntity::class.java).list().map { it.toDomain(json) }
}
