package com.tolmol.user.api.controllers

import com.tolmol.user.api.dto.SocialLoginRequest
import com.tolmol.user.service.SocialAuthService
import org.springframework.web.bind.annotation.*

@RestController
class SocialAuthController(private val auth: SocialAuthService) {
    @PostMapping("/api/auth/social")
    fun login(@RequestBody request: SocialLoginRequest) =
        mapOf("accessToken" to auth.login(request.provider,request.token))
}
