package com.tolmol.marketplace.api.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
data class ListingRequest(val category: String,val weight: BigDecimal,val startingRate: BigDecimal,val deadline: Instant)
data class ListingDto(val id: UUID,val category: String,val weight: BigDecimal,val startingRate: BigDecimal,val deadline: Instant,val status: String)
