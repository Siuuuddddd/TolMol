package com.tolmol.user.api.mappers

import com.tolmol.user.api.dto.LocationRequest
import com.tolmol.user.domain.model.Position
fun LocationRequest.toDomain() = Position(latitude,longitude)
