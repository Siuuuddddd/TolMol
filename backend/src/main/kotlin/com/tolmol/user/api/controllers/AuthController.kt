package com.tolmol.user.api.controllers

import com.tolmol.user.api.dto.*
import com.tolmol.user.api.mappers.toDomain
import com.tolmol.user.domain.model.Role
import com.tolmol.user.service.AuthService
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api")
class AuthController(private val auth: AuthService) {
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody req: RegisterRequest) = mapOf("id" to auth.register(req.email,req.password,req.roles,req.name))
    @PostMapping("/auth/login")
    fun login(@RequestBody req: LoginRequest) = mapOf("accessToken" to auth.login(req.email,req.password))
    @GetMapping("/me")
    fun profile(p: Principal): ProfileDto {
        val id = UUID.fromString(p.name)
        return ProfileDto(id, auth.roles(id))
    }
    @PutMapping("/me/location/{role}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun locate(p: Principal,@PathVariable role: Role,@RequestBody req: LocationRequest) =
        auth.locate(UUID.fromString(p.name),role,req.toDomain())
}
