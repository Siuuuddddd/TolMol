package com.tolmol.deal.api.dto

import java.math.BigDecimal
import java.util.UUID
data class WeightRequest(val weight: BigDecimal)
data class ProposalRequest(val amount: BigDecimal)
data class ConfirmRequest(val revision: Long)
data class DealDto(val listing: UUID,val state: String,val buyer: UUID,val seller: UUID,val rate: BigDecimal,
    val estimatedWeight: BigDecimal,val actualWeight: BigDecimal?,val amount: BigDecimal?,val proposedBy: UUID?,
    val revision: Long,val relisted: UUID?,val pickupLatitude: Double,val pickupLongitude: Double,val coverPhotoUrl: String? = null,val photoUrls: List<String> = emptyList(),val imageKey: String? = null,val category: String = "OTHER")
