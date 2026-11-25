package com.tolmol.user.api.controllers

import com.tolmol.user.service.GoogleAuthService
import org.springframework.web.bind.annotation.*

data class GoogleLoginRequest(val idToken: String)

@RestController
class GoogleAuthController(private val auth: GoogleAuthService) {
    @PostMapping("/auth/google")
    fun login(@RequestBody request: GoogleLoginRequest) = mapOf("accessToken" to auth.login(request.idToken))
}
