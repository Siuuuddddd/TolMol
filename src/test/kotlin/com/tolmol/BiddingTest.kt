package com.tolmol
import com.tolmol.bidding.service.BiddingService
import com.tolmol.bidding.domain.exception.*
import com.tolmol.user.domain.model.Role
import com.tolmol.platform.infra.config.Databases
import com.tolmol.platform.infra.kafka.EventOutbox
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

abstract class AuctionFixture : MarketFixture() {
    @Autowired lateinit var bidding: BiddingService
    fun bid(buyer: UUID,id: UUID,rate: Int) = bidding.place(buyer,id,rate.toBigDecimal(),UUID.randomUUID())
}
class BiddingTest : AuctionFixture() {
    @Autowired lateinit var db: Databases
    @Autowired lateinit var relay: EventOutbox
    @Test fun deadlineAwardsHighestAndExpiresEmptyLot() {
        val seller = user(Role.SELLER)
        val open = lot(seller).id
        val empty = lot(seller).id
        bid(user(Role.BUYER),open,12)
        val highest = bid(user(Role.BUYER),open,17)
        db.marketplace.sql.sql("update listings set deadline=now()-interval '1 second' where id in (?,?)").params(open,empty).update()
        bidding.expire()
        assertEquals(highest.buyer,bidding.winningBuyer(open))
        assertEquals("EXPIRED",bidding.snapshot(seller,empty).status)
    }
    @Test fun ascendingOffersCooldownAwardAndPublication() {
        val seller = user(Role.SELLER)
        val id = lot(seller).id
        val buyer = user(Role.BUYER)
        val key = UUID.randomUUID()
        val first = bidding.place(buyer,id,12.toBigDecimal(),key)
        assertEquals(first.id,bidding.place(buyer,id,12.toBigDecimal(),key).id)
        assertThrows(CooldownException::class.java) { bid(buyer,lot(seller).id,14) }
        bid(buyer,lot(user(Role.SELLER)).id,12)
        db.marketplace.sql.sql("update bids set created_at=now()-interval '6 hours' where id=?").param(first.id).update()
        bid(buyer,lot(seller).id,12)
        val other = user(Role.BUYER)
        assertThrows(InvalidBidException::class.java) { bid(other,id,12) }
        bid(other,id,15)
        assertEquals(15,bidding.snapshot(seller,id).highest!!.toInt())
        bidding.accept(seller,id,first.id)
        assertEquals("AWARDED",bidding.snapshot(seller,id).status)
        assertThrows(AuctionClosedException::class.java) { bid(other,id,16) }
        relay.publishMarketplace()
        assertEquals(6L,db.marketplace.sql.sql("select count(*) from outbox where aggregate_id=? and published and event_type in ('BID_PLACED','LEADER_CHANGED','AUCTION_AWARDED','STATUS_CHANGED')").param(id).query(Long::class.java).single())
    }
}
