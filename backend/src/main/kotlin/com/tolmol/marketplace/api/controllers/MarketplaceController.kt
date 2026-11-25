package com.tolmol.marketplace.api.controllers

import com.tolmol.marketplace.api.dto.*
import com.tolmol.marketplace.api.mappers.*
import com.tolmol.marketplace.service.MarketplaceService
import org.springframework.web.bind.annotation.*
import java.util.UUID
import java.security.Principal
@RestController
@RequestMapping("/api/listings")
class MarketplaceController(private val market: MarketplaceService) {
    @PostMapping fun publish(p: Principal,@RequestBody req: ListingRequest) = market.publish(UUID.fromString(p.name),req.toDomain()).toDto()
    @GetMapping fun feed(p: Principal) = market.feedDetails(UUID.fromString(p.name)).map { it.toDto() }
    @GetMapping("/watchlist") fun watchlist(p: Principal) = market.feedDetails(UUID.fromString(p.name),true).map { it.toDto() }
    @GetMapping("/{id}") fun detail(p: Principal,@PathVariable id: UUID) = market.describe(UUID.fromString(p.name),id).toDto()
    @PutMapping("/{id}/watch") fun watch(p: Principal,@PathVariable id: UUID) = market.watch(UUID.fromString(p.name),id,true)
    @DeleteMapping("/{id}/watch") fun unwatch(p: Principal,@PathVariable id: UUID) = market.watch(UUID.fromString(p.name),id,false)
}
