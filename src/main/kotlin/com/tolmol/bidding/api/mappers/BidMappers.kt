package com.tolmol.bidding.api.mappers

import com.tolmol.bidding.api.dto.BidDto
import com.tolmol.bidding.domain.model.Bid
fun Bid.toDto() = BidDto(id,buyer,rate,createdAt)
