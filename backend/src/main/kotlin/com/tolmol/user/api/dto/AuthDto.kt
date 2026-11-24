package com.tolmol.user.api.dto

import com.tolmol.user.domain.model.Role
data class RegisterRequest(val email: String,val password: String,val roles: Set<Role>)
data class LoginRequest(val email: String,val password: String)
data class LocationRequest(val latitude: Double,val longitude: Double)
