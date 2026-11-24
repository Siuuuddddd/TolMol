package com.tolmol.marketplace.infra.database.entities

import java.util.UUID
import java.math.BigDecimal
import java.time.Instant
data class ListingEntity(val id: UUID,val seller: UUID,val category: String,val weight: BigDecimal,val startingRate: BigDecimal,
    val latitude: Double,val longitude: Double,val deadline: Instant,val status: String,val version: Long)
