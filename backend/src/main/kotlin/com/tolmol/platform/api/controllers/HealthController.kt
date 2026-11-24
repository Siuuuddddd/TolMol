package com.tolmol.platform.api.controllers
import org.springframework.web.bind.annotation.*

@RestController
class HealthController {
    @GetMapping("/health")
    fun health() = mapOf("status" to "UP")
}
