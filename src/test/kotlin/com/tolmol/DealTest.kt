package com.tolmol
import com.tolmol.deal.service.DealService
import com.tolmol.deal.domain.exception.*
import com.tolmol.deal.domain.model.DealState
import com.tolmol.chat.domain.exception.ChatClosedException
import com.tolmol.user.domain.model.Role
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class DealTest : SocketFixture() {
    @Autowired lateinit var deals: DealService

    @Test fun pickupAndNegotiatedSettlement() {
        val seller = user(Role.SELLER)
        val buyer = user(Role.BUYER)
        val listing = lot(seller).id
        assertTrue(market.feed(buyer).any { it.id==listing })
        bidding.accept(seller,listing,bid(buyer,listing,12).id)
        assertEquals(DealState.PENDING_PICKUP,deals.find(buyer,listing).state)
        assertThrows(DealUnavailableException::class.java) { deals.find(UUID.randomUUID(),listing) }
        val thread = chat.open(buyer,listing)
        assertEquals(DealState.WEIGHT_DISPUTE,deals.inspect(buyer,listing,17.toBigDecimal()).state)
        chat.send(seller,thread.id,"Can we agree on 200?",UUID.randomUUID())
        val first = deals.propose(seller,listing,200.toBigDecimal())
        val counter = deals.propose(buyer,listing,195.toBigDecimal())
        assertThrows(DealConflictException::class.java) { deals.confirm(seller,listing,first.revision) }
        assertThrows(DealConflictException::class.java) { deals.confirm(buyer,listing,counter.revision) }
        val done = deals.confirm(seller,listing,counter.revision)
        assertEquals(DealState.COMPLETED,done.state)
        assertEquals(195,done.amount!!.toInt())
        assertThrows(ChatClosedException::class.java) { chat.send(seller,thread.id,"Another message",UUID.randomUUID()) }
    }

    @Test fun correctWeightAndCancelledRelist() {
        val buyer = user(Role.BUYER)
        val seller = user(Role.SELLER)
        val id = lot(seller).id
        bidding.accept(seller,id,bid(buyer,id,15).id)
        val inspected = deals.inspect(buyer,id,20.toBigDecimal())
        assertEquals(300,inspected.amount!!.toInt())
        assertEquals(DealState.COMPLETED,deals.confirm(seller,id,inspected.revision).state)

        val other = user(Role.SELLER)
        val disputed = lot(other).id
        bidding.accept(other,disputed,bid(buyer,disputed,12).id)
        deals.inspect(buyer,disputed,16.toBigDecimal())
        val cancelled = deals.cancel(other,disputed)
        assertEquals(DealState.CANCELLED_RELISTED,cancelled.state)
        val newLot = market.feed(buyer).single { it.id==cancelled.relisted }
        assertEquals(16,newLot.weight.toInt())
        assertEquals("OPEN",newLot.status)
        assertThrows(DealConflictException::class.java) { deals.cancel(buyer,disputed) }
    }
}
