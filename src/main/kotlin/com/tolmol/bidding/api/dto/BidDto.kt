package com.tolmol.bidding.api.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
data class BidRequest(val rate: BigDecimal,val requestKey: UUID)
data class AwardRequest(val bidId: UUID)
data class BidDto(val id: UUID,val buyer: UUID,val rate: BigDecimal,val createdAt: Instant)
