package com.tolmol.deal.infra.database.repositories

import com.tolmol.deal.domain.model.*
import com.tolmol.deal.domain.exception.DealUnavailableException
import com.tolmol.deal.infra.database.entities.DealEntity
import com.tolmol.deal.infra.database.mappers.toDomain
import com.tolmol.deal.service.DealStore
import com.tolmol.platform.infra.config.Databases
import org.springframework.stereotype.Repository
import java.util.UUID
import java.math.BigDecimal

@Repository
class DealRepository(dbs: Databases) : DealStore {
    private val db = dbs.marketplace
    private val select = "select d.*,l.latitude,l.longitude from deals d join listings l on l.id=d.listing"
    private fun read(id: UUID,lock: Boolean): Deal {
        val suffix = if(lock) " for update of d" else ""
        return db.sql.sql("$select where d.listing=?$suffix").param(id).query(DealEntity::class.java)
            .optional().orElse(null)?.toDomain() ?: throw DealUnavailableException()
    }
    override fun find(listing: UUID) = read(listing,false)
    override fun mine(user: UUID) = db.sql.sql("$select where d.seller=? or d.buyer=? order by l.deadline desc")
        .params(user,user).query(DealEntity::class.java).list().map { it.toDomain() }
    override fun change(listing: UUID,transition: (Deal)->Deal): Deal = db.transactions.execute {
        val changed = transition(read(listing,true))
        db.sql.sql("""update deals set state=?, actual_weight=?,amount=?,proposed_by=?,relisted=?,revision=revision+1 where listing=?""")
            .params(listOf(changed.state.name,changed.actualWeight,changed.amount,changed.proposedBy,changed.relisted,listing)).update()
        if (changed.state in setOf(DealState.COMPLETED,DealState.CANCELLED_RELISTED)) {
            db.sql.sql("update listings set status=?,version=version+1 where id=?").params(changed.state.name,listing).update()
        }
        read(listing,false)
    }!!
    override fun relist(deal: Deal,weight: BigDecimal): UUID {
        val id = UUID.randomUUID()
        db.sql.sql("""insert into listings(id,seller,category,weight,starting_rate,latitude,longitude,deadline,title,description,image_key)
            select ?,seller,category,?,starting_rate,latitude,longitude,now()+interval '24 hours',title,description,image_key from listings where id=?""")
            .params(id,weight,deal.listing).update()
        return id
    }
}
