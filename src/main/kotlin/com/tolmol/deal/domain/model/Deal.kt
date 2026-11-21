package com.tolmol.deal.domain.model

import java.math.BigDecimal
import java.util.UUID

enum class DealState { PENDING_PICKUP, WEIGHT_DISPUTE, COMPLETED, CANCELLED_RELISTED }
data class Deal(val listing: UUID,val seller: UUID,val buyer: UUID,val rate: BigDecimal,val estimatedWeight: BigDecimal,
    val actualWeight: BigDecimal?,val amount: BigDecimal?,val proposedBy: UUID?,val revision: Long,val state: DealState,
    val relisted: UUID?,val latitude: Double,val longitude: Double)
