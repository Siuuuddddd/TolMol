package com.tolmol.bidding.service

import com.tolmol.bidding.domain.model.*
import com.tolmol.bidding.domain.exception.*
import com.tolmol.marketplace.service.MarketplaceService
import org.springframework.stereotype.Service
import java.time.Instant
import java.math.BigDecimal
import java.util.UUID

@Service
class BiddingService(private val store: BidStore,private val market: MarketplaceService) {
    fun place(buyer: UUID,id: UUID,rate: BigDecimal,key: UUID): Bid = store.atomic {
        store.lockBuyer(buyer)
        val previous = store.previous(buyer,key)
        if (previous != null) {
            if (previous.listing != id || previous.rate.compareTo(rate)!=0) throw RequestConflictException()
            return@atomic previous
        }
        market.eligible(buyer,id)
        val auction = store.auction(id,true)
        val now = Instant.now()
        if (auction.status!="OPEN" || now >= auction.deadline) throw AuctionClosedException()
        val next = store.nextBid(buyer,auction.seller)
        if (next != null && now < next) throw CooldownException(next)
        val leader = store.leading(id)
        if (rate.scale()>2 || rate < auction.startingRate || (leader!=null && rate<=leader.rate)) throw InvalidBidException()
        val bid = Bid(UUID.randomUUID(),id,buyer,rate,key,now)
        store.save(bid,auction.seller)
        store.change(auction,"OPEN","BID_PLACED",bid)
        bid
    }
    fun accept(seller: UUID,id: UUID,bidId: UUID) = store.atomic {
        val auction = store.auction(id,true)
        if (auction.seller!=seller) throw AuctionAccessException()
        if (auction.status!="OPEN" || Instant.now()>=auction.deadline) throw AuctionClosedException()
        val bid = store.bids(id).find { it.id==bidId } ?: throw InvalidBidException()
        store.change(auction,"AWARDED","AUCTION_AWARDED",bid)
    }
    fun expire() {
        for (id in store.expired()) store.atomic {
            val auction = store.auction(id,true)
            if (auction.status=="OPEN" && Instant.now()>=auction.deadline) {
                val leader = store.leading(id)
                if (leader==null) store.change(auction,"EXPIRED","AUCTION_EXPIRED",null)
                else store.change(auction,"AWARDED","AUCTION_AWARDED",leader)
            }
        }
    }
    fun winningBuyer(id: UUID) = store.winningBuyer(id)
    fun bids(seller: UUID,id: UUID): List<Bid> {
        if (store.auction(id).seller!=seller) throw AuctionAccessException()
        return store.bids(id)
    }
    fun snapshot(user: UUID,id: UUID) = store.atomic {
        val auction = store.auction(id,true)
        if (user!=auction.seller) market.eligible(user,id)
        store.snapshot(auction)
    }
}
