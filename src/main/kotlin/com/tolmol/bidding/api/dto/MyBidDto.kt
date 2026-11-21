package com.tolmol.bidding.api.dto

import java.util.UUID
import java.math.BigDecimal
import java.time.Instant
data class MyBidDto(val id: UUID,val listing: UUID,val rate: BigDecimal,val createdAt: Instant)
