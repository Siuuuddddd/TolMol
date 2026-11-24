package com.tolmol.user.infra.database.entities

import java.util.UUID
data class UserEntity(val id: UUID, val email: String, val passwordHash: String, val roles: String)
