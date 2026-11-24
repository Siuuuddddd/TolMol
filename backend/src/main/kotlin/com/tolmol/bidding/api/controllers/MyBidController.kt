package com.tolmol.bidding.api.controllers

import com.tolmol.bidding.service.BiddingService
import com.tolmol.bidding.api.dto.MyBidDto
import org.springframework.web.bind.annotation.*
import java.util.UUID
import java.security.Principal

@RestController
class MyBidController(private val service: BiddingService) {
    @GetMapping("/api/me/bids")
    fun mine(p: Principal,@RequestParam(required=false) after: UUID?,@RequestParam(defaultValue="25") limit: Int) =
        service.mine(UUID.fromString(p.name),after,limit).map { MyBidDto(it.id,it.listing,it.rate,it.createdAt,it.status) }
}
