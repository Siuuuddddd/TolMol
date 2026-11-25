package com.tolmol.user.api.dto

import com.tolmol.user.domain.model.Role
import java.util.UUID
data class ProfileDto(val id: UUID, val roles: Set<Role>)
data class RegisterRequest(val email: String,val password: String,val roles: Set<Role>,val name: String? = null)
data class SocialLoginRequest(val provider: String, val token: String)
data class LoginRequest(val email: String,val password: String)
data class LocationRequest(val latitude: Double,val longitude: Double)
