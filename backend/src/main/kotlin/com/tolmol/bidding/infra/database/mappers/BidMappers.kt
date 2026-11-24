package com.tolmol.bidding.infra.database.mappers

import com.tolmol.bidding.domain.model.Bid
import com.tolmol.bidding.infra.database.entities.BidEntity
fun BidEntity.toDomain() = Bid(id,listing,buyer,rate,requestKey,createdAt)
