package com.tolmol.bidding.infra.database.entities

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
data class BidEntity(val id: UUID,val listing: UUID,val buyer: UUID,val rate: BigDecimal,val requestKey: UUID,val createdAt: Instant)
