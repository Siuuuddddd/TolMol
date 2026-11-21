package com.tolmol.deal.infra.database.mappers

import com.tolmol.deal.domain.model.*
import com.tolmol.deal.infra.database.entities.DealEntity
fun DealEntity.toDomain() = Deal(listing,seller,buyer,rate,estimatedWeight,actualWeight,amount,proposedBy,revision,
    DealState.valueOf(state),relisted,latitude,longitude)
