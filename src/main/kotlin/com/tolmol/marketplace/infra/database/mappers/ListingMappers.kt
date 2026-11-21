package com.tolmol.marketplace.infra.database.mappers

import com.tolmol.marketplace.infra.database.entities.ListingEntity
import com.tolmol.marketplace.domain.model.Listing
fun ListingEntity.toDomain() = Listing(id,seller,category,weight,startingRate,latitude,longitude,deadline,status,version)
