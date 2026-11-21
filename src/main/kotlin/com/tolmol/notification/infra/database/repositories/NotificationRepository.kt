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
    override fun inbox(user: UUID,before: Long?,limit: Int) = sql.sql("""select * from alerts where recipient=?
        and (cast(? as bigint) is null or sequence<?) order by sequence desc limit ?""")
        .params(listOf(user,before,before,limit)).query(AlertEntity::class.java).list().map { it.toDomain(json) }
    override fun unread(user: UUID) = sql.sql("select count(*) from alerts where recipient=? and not is_read")
        .param(user).query(Long::class.java).single()
    override fun markRead(user: UUID,id: UUID?) {
        sql.sql("update alerts set is_read=true where recipient=? and (cast(? as uuid) is null or event_id=?)")
            .params(listOf(user,id,id)).update()
    }
    override fun muteChat(user: UUID,muted: Boolean) {
        sql.sql("insert into notification_preferences values(?,?) on conflict(user_id) do update set mute_chat=excluded.mute_chat")
            .params(user,muted).update()
    }
    override fun muteListing(user: UUID,listing: UUID,muted: Boolean) {
        val statement = if (muted) "insert into muted_listings values(?,?) on conflict do nothing"
            else "delete from muted_listings where user_id=? and listing=?"
        sql.sql(statement).params(user,listing).update()
    }
    override fun muted(user: UUID,listing: UUID,chat: Boolean) = sql.sql("""select exists(
        select 1 from muted_listings where user_id=? and listing=?) or
        (? and exists(select 1 from notification_preferences where user_id=? and mute_chat))""")
        .params(user,listing,chat,user).query(Boolean::class.java).single()
}
