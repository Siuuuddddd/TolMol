package com.tolmol
import com.tolmol.user.domain.model.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class RecoveryTest : SocketFixture() {
    @Test fun ownersRecoverOnlyTheirOwnWork() {
        val seller = user(Role.SELLER)
        val stranger = user(Role.SELLER)
        val buyer = user(Role.BUYER)
        val listing = lot(seller).id
        lot(seller)
        val bid = bid(buyer,listing,12)
        val thread = chat.open(buyer,listing)
        val first = market.mine(seller,null,1).single()
        assertNotEquals(first.id,market.mine(seller,first.id,1).single().id)
        assertEquals(bid.id,bidding.mine(buyer,null,10).single().id)
        assertEquals("LEADING",bidding.mine(buyer,null,10).single().status)
        bidding.accept(seller,listing,bid.id)
        assertEquals("WON",bidding.mine(buyer,null,10).single().status)
        assertEquals(thread.id,chat.mine(seller,null,10).single().id)
        assertTrue(market.mine(stranger,null,10).isEmpty())
        assertTrue(bidding.mine(stranger,null,10).isEmpty())
        assertTrue(chat.mine(stranger,null,10).isEmpty())
    }
}
