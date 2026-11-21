package com.tolmol.bidding.infra.database.repositories

import com.tolmol.bidding.domain.model.*
import com.tolmol.bidding.service.BidStore
import com.tolmol.bidding.infra.database.entities.BidEntity
import com.tolmol.bidding.infra.database.mappers.toDomain
import com.tolmol.bidding.infra.redis.AuctionCache
import com.tolmol.marketplace.domain.exception.ListingUnavailableException
import com.tolmol.platform.infra.config.Databases
import com.tolmol.platform.infra.kafka.EventOutbox
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID
import java.sql.Timestamp

@Repository
class BidRepository(dbs: Databases,private val events: EventOutbox,private val cache: AuctionCache) : BidStore {
    private val db = dbs.marketplace
    private val sql = db.sql
    override fun mine(user: UUID,after: UUID?,limit: Int): List<Bid> = sql.sql("""select * from bids where buyer=?
        and (cast(? as uuid) is null or id>?) order by id limit ?""").params(listOf(user,after,after,limit))
        .query(BidEntity::class.java).list().map { it.toDomain() }
    override fun <T:Any> atomic(work: ()->T): T = db.transactions.execute { work() }!!
    override fun auction(id: UUID,lock: Boolean): Auction {
        val query = "select id as listing,seller,starting_rate,deadline,status,version from listings where id=?" + if(lock) " for update" else ""
        return sql.sql(query).param(id).query(Auction::class.java).optional().orElse(null) ?: throw ListingUnavailableException()
    }
    override fun lockBuyer(buyer: UUID) {
        sql.sql("select pg_advisory_xact_lock(?)").param(buyer.mostSignificantBits xor buyer.leastSignificantBits).query { _,_ -> true }.single()
    }
    override fun previous(buyer: UUID,key: UUID) = sql.sql("select * from bids where buyer=? and request_key=?")
        .params(buyer,key).query(BidEntity::class.java).optional().orElse(null)?.toDomain()
    override fun leading(id: UUID) = sql.sql("select * from bids where listing=? order by rate desc limit 1")
        .param(id).query(BidEntity::class.java).optional().orElse(null)?.toDomain()
    override fun bids(id: UUID) = sql.sql("select * from bids where listing=? order by rate desc").param(id)
        .query(BidEntity::class.java).list().map { it.toDomain() }
    override fun nextBid(buyer: UUID,seller: UUID) = sql.sql("select created_at from bids where buyer=? and seller=? order by created_at desc limit 1")
        .params(buyer,seller).query(Timestamp::class.java).optional().orElse(null)?.toInstant()?.plusSeconds(21600)
    override fun save(bid: Bid,seller: UUID) {
        sql.sql("insert into bids values(?,?,?,?,?,?,?)").params(bid.id,bid.listing,bid.buyer,seller,bid.rate,bid.requestKey,Timestamp.from(bid.createdAt)).update()
    }
    override fun change(auction: Auction,status: String,event: String,bid: Bid?) {
        val id = auction.listing
        sql.sql("update listings set status=?,version=version+1 where id=?").params(status,id).update()

        val recipients = sql.sql("select buyer from watchers where listing=? union select buyer from bids where listing=?")
            .params(id,id).query(UUID::class.java).set() + auction.seller
        val leader = leading(id)
        val fields = mutableMapOf("version" to (auction.version+1).toString(),"status" to status)
        leader?.let { fields["highestRate"] = it.rate.toPlainString(); fields["leaderAlias"] = it.id.toString() }
        bid?.let { fields["bidId"] = it.id.toString(); fields["rate"] = it.rate.toPlainString() }
        events.append(db,event,id,recipients,fields)
        if (event=="BID_PLACED") events.append(db,"LEADER_CHANGED",id,recipients,fields)
        else events.append(db,"STATUS_CHANGED",id,recipients,fields)
        if (event=="AUCTION_AWARDED" && bid!=null) {
            val bidders = bids(id).map { it.buyer }.toSet()
            events.append(db,"BID_WON",id,setOf(bid.buyer),fields)
            if ((bidders-bid.buyer).isNotEmpty()) events.append(db,"BID_LOST",id,bidders-bid.buyer,fields)
        }
        if (event=="BID_PLACED" && bid!=null) {
            val former = sql.sql("select buyer from bids where listing=? and id<>? order by rate desc limit 1")
                .params(id,bid.id).query(UUID::class.java).optional().orElse(null)
            if (former!=null && former!=bid.buyer) events.append(db,"OUTBID",id,setOf(former),fields)
        }
        if (status=="AWARDED") sql.sql("update listings set winning_bid=? where id=?").params(requireNotNull(bid).id,id).update()
        val updated = auction.copy(status=status,version=auction.version+1)
        com.tolmol.platform.infra.config.afterCommit {
            cache.read(updated) { Snapshot(id,leader?.rate,leader?.id,status,updated.version) }
        }
    }
    override fun winningBuyer(id: UUID) = sql.sql("select buyer from bids join listings on winning_bid=bids.id where listings.id=?")
        .param(id).query(UUID::class.java).optional().orElse(null)
    override fun expired() = sql.sql("select id from listings where status='OPEN' and deadline<=now() order by deadline limit 100").query(UUID::class.java).list()
    override fun snapshot(auction: Auction) = cache.read(auction) {
        val leader = leading(auction.listing)
        Snapshot(auction.listing,leader?.rate,leader?.id,auction.status,auction.version)
    }
}
