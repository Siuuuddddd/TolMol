package com.tolmol.deal.api.controllers

import com.tolmol.deal.api.dto.*
import com.tolmol.deal.api.mappers.toDto
import com.tolmol.deal.service.DealService
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/deals")
class DealController(private val deals: DealService) {
    @GetMapping fun mine(p: Principal) = deals.mine(UUID.fromString(p.name)).map { it.toDto() }
    @GetMapping("/{id}") fun get(p: Principal,@PathVariable id: UUID) = deals.find(UUID.fromString(p.name),id).toDto()
    @PostMapping("/{id}/inspect") fun inspect(p: Principal,@PathVariable id: UUID,@RequestBody req: WeightRequest) =
        deals.inspect(UUID.fromString(p.name),id,req.weight).toDto()
    @PostMapping("/{id}/propose") fun propose(p: Principal,@PathVariable id: UUID,@RequestBody req: ProposalRequest) =
        deals.propose(UUID.fromString(p.name),id,req.amount).toDto()
    @PostMapping("/{id}/confirm") fun confirm(p: Principal,@PathVariable id: UUID,@RequestBody req: ConfirmRequest) =
        deals.confirm(UUID.fromString(p.name),id,req.revision).toDto()
    @PostMapping("/{id}/cancel") fun cancel(p: Principal,@PathVariable id: UUID) = deals.cancel(UUID.fromString(p.name),id).toDto()
}
