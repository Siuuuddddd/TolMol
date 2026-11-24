package com.tolmol.marketplace.api.mappers

import com.tolmol.marketplace.api.dto.*
import com.tolmol.marketplace.domain.model.*
fun ListingRequest.toDomain() = LotDraft(category,weight,startingRate,deadline)
fun Listing.toDto() = ListingDto(id,category,weight,startingRate,deadline,status)
