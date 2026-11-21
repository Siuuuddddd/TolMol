package com.tolmol.marketplace.api.controllers

import com.tolmol.marketplace.service.MarketplaceService
import com.tolmol.marketplace.api.mappers.toDto
import org.springframework.web.bind.annotation.*
import java.util.UUID
import java.security.Principal

@RestController
class MyListingController(private val service: MarketplaceService) {
    @GetMapping("/api/me/listings")
    fun mine(p: Principal,@RequestParam(required=false) after: UUID?,@RequestParam(defaultValue="25") limit: Int) =
        service.mine(UUID.fromString(p.name),after,limit).map { it.toDto() }
}
