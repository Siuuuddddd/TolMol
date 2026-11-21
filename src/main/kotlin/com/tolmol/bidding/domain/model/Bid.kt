package com.tolmol.bidding.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
data class Bid(val id: UUID,val listing: UUID,val buyer: UUID,val rate: BigDecimal,val requestKey: UUID,val createdAt: Instant)
data class Auction(val listing: UUID,val seller: UUID,val startingRate: BigDecimal,val deadline: Instant,val status: String,val version: Long)
data class Snapshot(val listing: UUID,val highest: BigDecimal?,val leader: UUID?,val status: String,val version: Long)
