package com.tolmol.bidding.service

import com.tolmol.bidding.domain.model.*
import java.util.UUID
import java.time.Instant
interface BidStore {
    fun mine(user: UUID,after: UUID?,limit: Int): List<Bid>
    fun <T:Any> atomic(work: ()->T): T
    fun auction(id: UUID,lock: Boolean = false): Auction
    fun previous(buyer: UUID,key: UUID): Bid?
    fun leading(id: UUID): Bid?
    fun winningBuyer(id: UUID): UUID?
    fun bids(id: UUID): List<Bid>
    fun lockBuyer(buyer: UUID)
    fun nextBid(buyer: UUID,seller: UUID): Instant?
    fun save(bid: Bid,seller: UUID)
    fun change(auction: Auction,status: String,event: String,bid: Bid?)
    fun snapshot(auction: Auction): Snapshot
    fun expired(): List<UUID>
}
