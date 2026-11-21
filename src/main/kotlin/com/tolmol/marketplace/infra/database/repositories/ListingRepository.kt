package com.tolmol.marketplace.infra.database.repositories

import com.tolmol.marketplace.domain.model.*
import com.tolmol.marketplace.service.ListingStore
import com.tolmol.marketplace.infra.database.entities.ListingEntity
import com.tolmol.marketplace.infra.database.mappers.toDomain
import com.tolmol.marketplace.infra.redis.ListingGeoIndex
import com.tolmol.platform.infra.config.Databases
import com.tolmol.user.domain.model.Position
import org.springframework.stereotype.Repository
import java.util.UUID
import java.sql.Timestamp

@Repository
class ListingRepository(db: Databases,private val index: ListingGeoIndex) : ListingStore {
    private val sql = db.marketplace.sql
    override fun mine(user: UUID,after: UUID?,limit: Int): List<Listing> = sql.sql("""select * from listings where seller=?
        and (cast(? as uuid) is null or id>?) order by id limit ?""").params(listOf(user,after,after,limit))
        .query(ListingEntity::class.java).list().map { it.toDomain() }
    override fun insert(listing: Listing) {
        with(listing) { sql.sql("""insert into listings(id,seller,category,weight,starting_rate,latitude,longitude,deadline)
            values(?,?,?,?,?,?,?,?)""").params(id,seller,category,weight,startingRate,latitude,longitude,Timestamp.from(deadline)).update() }
        index.add(listing)
    }
    override fun get(id: UUID) = sql.sql("select * from listings where id=?").param(id)
        .query(ListingEntity::class.java).optional().orElse(null)?.toDomain()
    override fun nearby(position: Position): List<Listing> {
        val count = sql.sql("select count(*) from listings where abs(latitude)<=85").query(Long::class.java).single()
        val ids = index.candidates(count,position) {
            sql.sql("select * from listings").query(ListingEntity::class.java).list().map { it.toDomain() }
        }
        val open = "select * from listings where status='OPEN' and deadline>now()"
        if (ids != null && ids.isEmpty()) return emptyList()
        val query = if (ids == null) sql.sql(open) else sql.sql("$open and id in (:ids)").param("ids",ids)
        return query.query(ListingEntity::class.java).list().map { it.toDomain() }
            .filter { it.distanceKm(position) <= 20 }.sortedBy { it.distanceKm(position) }
    }
    override fun watchedIds(buyer: UUID) = sql.sql("select listing from watchers where buyer=?").param(buyer)
        .query(UUID::class.java).set()
    override fun watch(id: UUID,buyer: UUID,enabled: Boolean) {
        val query = if (enabled) "insert into watchers values(?,?) on conflict do nothing" else "delete from watchers where listing=? and buyer=?"
        sql.sql(query).params(id,buyer).update()
    }
}
