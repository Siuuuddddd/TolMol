package com.tolmol.user.infra.database.mappers

import com.tolmol.user.domain.model.*
import com.tolmol.user.infra.database.entities.UserEntity
fun UserEntity.toDomain() = Account(id,email,passwordHash,roles.split(",").map(Role::valueOf).toSet())
