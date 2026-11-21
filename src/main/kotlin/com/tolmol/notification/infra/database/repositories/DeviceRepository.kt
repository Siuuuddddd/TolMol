package com.tolmol.notification.infra.database.repositories
import com.tolmol.notification.domain.model.*
import com.tolmol.notification.service.DeviceStore
import com.tolmol.platform.domain.exception.BusinessException
import com.tolmol.platform.infra.config.Databases
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DeviceRepository(dbs: Databases) : DeviceStore {
    private val sql = dbs.users.sql
    override fun register(user: UUID,platform: Platform,token: String): UUID = sql.sql("""insert into devices values(?,?,?,?)
        on conflict(platform,token) do update set token=excluded.token where devices.owner_id=excluded.owner_id returning id""")
        .params(UUID.randomUUID(),user,platform.name,token).query(UUID::class.java).optional().orElseThrow { BusinessException(409,"Device already registered") }
    override fun remove(user: UUID,id: UUID) {
        sql.sql("delete from devices where owner_id=? and id=?").params(user,id).update()
    }
    override fun devices(user: UUID) = sql.sql("select id,platform,token from devices where owner_id=?").param(user)
        .query { rs,_ -> Device(rs.getObject("id",UUID::class.java),Platform.valueOf(rs.getString("platform")),rs.getString("token")) }.list()
}
