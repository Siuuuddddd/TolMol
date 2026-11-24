package com.tolmol.deal.api.mappers

import com.tolmol.deal.domain.model.Deal
import com.tolmol.deal.api.dto.DealDto
fun Deal.toDto() = DealDto(listing,state.name,buyer,seller,rate,estimatedWeight,actualWeight,amount,proposedBy,revision,relisted,latitude,longitude)
