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
import com.tolmol.user.service.SocialIdentity
import com.tolmol.user.service.GoogleIdentity
import com.tolmol.platform.domain.exception.BusinessException

@Repository
class UserRepository(private val db: Databases, private val cache: com.tolmol.user.infra.redis.LocationCache) : UserStore {
    override fun register(account: Account, name: String?) {
        try {
            db.users.transactions.execute {
                db.users.sql.sql("insert into accounts(id,email,password_hash,roles) values(?,?,?,?)").params(account.id,account.email,account.passwordHash,account.roles.joinToString(",")).update()
                if (name != null) db.users.sql.sql("insert into account_profiles(user_id,name) values(?,?)").params(account.id,name).update()
            }
        } catch (e: DuplicateKeyException) { throw EmailTakenException() }
    }
    override fun socialAccount(identity: SocialIdentity, passwordHash: String): UUID {
        try {
            return db.users.transactions.execute {
                val key = identity.provider + ":" + identity.subject
                db.users.sql.sql("select pg_advisory_xact_lock(hashtextextended(?,0))").param(key).query { _,_ -> true }.single()
                val existing = db.users.sql.sql("select user_id from social_identities where provider=? and subject=?")
                    .params(identity.provider,identity.subject).query(UUID::class.java).optional().orElse(null)
                if (existing != null) return@execute existing
                if (find(identity.email) != null) throw EmailTakenException()
                val id = UUID.randomUUID()
                db.users.sql.sql("insert into accounts(id,email,password_hash,roles) values(?,?,?,?)")
                    .params(id,identity.email,passwordHash,"BUYER,SELLER").update()
                db.users.sql.sql("insert into account_profiles(user_id,name) values(?,?)").params(id,identity.name).update()
                db.users.sql.sql("insert into social_identities(provider,subject,user_id) values(?,?,?)")
                    .params(identity.provider,identity.subject,id).update()
                id
            }!!
        } catch (error: DuplicateKeyException) { throw EmailTakenException() }
    }
    override fun googleAccount(identity: GoogleIdentity): UUID = db.users.transactions.execute {
        db.users.sql.sql("select pg_advisory_xact_lock(hashtextextended(?,0))")
            .param("GOOGLE:" + identity.subject).query { _,_ -> true }.single()
        val known = db.users.sql.sql("select id from accounts where google_subject=?")
            .param(identity.subject).query(UUID::class.java).optional().orElse(null)
        if (known != null) return@execute known
        db.users.sql.sql("select pg_advisory_xact_lock(hashtextextended(?,0))")
            .param("EMAIL:" + identity.email).query { _,_ -> true }.single()
        val candidate = UUID.randomUUID()
        db.users.sql.sql("""insert into accounts(id,email,password_hash,roles,google_subject,email_verified)
            values(?,?,null,'BUYER,SELLER',?,true) on conflict(email) do nothing""")
            .params(candidate,identity.email,identity.subject).update()
        val account = db.users.sql.sql("select id,google_subject from accounts where email=? for update")
            .param(identity.email).query { row,_ -> row.getObject("id",UUID::class.java) to row.getString("google_subject") }.single()
        if (account.second != null && account.second != identity.subject) {
            throw BusinessException(409,"This email is already linked to another Google account. Use the original sign-in method.")
        }
        db.users.sql.sql("update accounts set google_subject=?,email_verified=true where id=?")
            .params(identity.subject,account.first).update()
        db.users.sql.sql("insert into account_profiles(user_id,name) values(?,?) on conflict(user_id) do nothing")
            .params(account.first,identity.name).update()
        db.users.sql.sql("insert into social_identities(provider,subject,user_id) values('GOOGLE',?,?) on conflict(provider,subject) do nothing")
            .params(identity.subject,account.first).update()
        account.first
    }!!

    override fun display(id: UUID): Pair<String,java.time.Instant?> = db.users.sql.sql("""select coalesce(p.name,'TolMol member') as name,a.joined_at
        from accounts a left join account_profiles p on p.user_id=a.id where a.id=?""").param(id)
        .query { row,_ -> row.getString("name") to row.getTimestamp("joined_at")?.toInstant() }.optional().orElse("TolMol member" to null)
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
