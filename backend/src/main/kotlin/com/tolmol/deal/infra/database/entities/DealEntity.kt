package com.tolmol.deal.infra.database.entities

import java.math.BigDecimal
import java.util.UUID
data class DealEntity(val listing: UUID,val seller: UUID,val buyer: UUID,val rate: BigDecimal,val estimatedWeight: BigDecimal,
    val actualWeight: BigDecimal?,val amount: BigDecimal?,val proposedBy: UUID?,val revision: Long,val state: String,
    val relisted: UUID?,val latitude: Double,val longitude: Double)
