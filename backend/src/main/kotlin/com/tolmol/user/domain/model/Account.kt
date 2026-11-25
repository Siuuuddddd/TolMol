package com.tolmol.user.domain.model

import java.util.UUID
enum class Role { BUYER, SELLER }
data class Account(val id: UUID, val email: String, val passwordHash: String?, val roles: Set<Role>)
data class Position(val latitude: Double, val longitude: Double) {
    init { require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "Invalid coordinates" } }
}
