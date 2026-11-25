package com.tolmol.marketplace.api.mappers

import com.tolmol.marketplace.api.dto.*
import com.tolmol.marketplace.domain.model.*
fun ListingRequest.toDomain() = LotDraft(category,weight,startingRate,deadline,title,description,imageKey)
fun Listing.toDto() = ListingDto(id,category,weight,startingRate,deadline,status,title,description,imageKey)

fun com.tolmol.marketplace.service.ListingInfo.toDto() = lot.toDto().copy(
    distanceKm=distanceKm?.let { kotlin.math.round(it * 10) / 10 },
    sellerDisplayName=sellerName,sellerMemberSince=memberSince)
