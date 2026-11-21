package com.tolmol.user.infra.database.repositories

import com.tolmol.platform.infra.config.Databases
import com.tolmol.user.service.UserStore
import com.tolmol.user.domain.model.*
import com.tolmol.user.domain.exception.EmailTakenException
import com.tolmol.user.infra.database.entities.UserEntity
import com.tolmol.user.infra.database.mappers.toDomain
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRepository(private val db: Databases, private val cache: com.tolmol.user.infra.redis.LocationCache) : UserStore {
    override fun register(account: Account) {
        try {
            db.users.sql.sql("insert into accounts values(?,?,?,?)").params(account.id,account.email,account.passwordHash,account.roles.joinToString(",")).update()
        } catch (e: DuplicateKeyException) { throw EmailTakenException() }
    }
    override fun find(email: String) = db.users.sql.sql("select * from accounts where email=?").param(email)
        .query(UserEntity::class.java).optional().orElse(null)?.toDomain()
    override fun hasRole(id: UUID, role: Role) = db.users.sql.sql("select count(*) from accounts where id=? and ?=any(string_to_array(roles,','))")
        .params(id,role.name).query(Long::class.java).single() == 1L
    override fun savePosition(id: UUID, role: Role, position: Position) {
        db.location.sql.sql("""insert into positions(user_id,role,latitude,longitude) values(?,?,?,?)
            on conflict(user_id,role) do update set latitude=excluded.latitude, longitude=excluded.longitude,updated_at=now(),revision=positions.revision+1""")
            .params(id,role.name,position.latitude,position.longitude).update()
    }
    override fun recentPosition(id: UUID, role: Role): Position? {
        val revision = db.location.sql.sql("""select revision from positions where user_id=? and role=?
            and updated_at >= now()-interval '15 minutes'""").params(id,role.name)
            .query(Long::class.java).optional().orElse(null) ?: return null
        return cache.resolve(id,role,revision) {
            db.location.sql.sql("select latitude,longitude from positions where user_id=? and role=? and revision=?")
                .params(id,role.name,revision).query(Position::class.java).optional().orElse(null)
        }
    }
}
