package com.tolmol.bidding.api.controllers

import com.tolmol.bidding.api.dto.*
import com.tolmol.bidding.api.mappers.toDto
import com.tolmol.bidding.service.BiddingService
import org.springframework.web.bind.annotation.*
import java.util.UUID
import java.security.Principal

@RestController
@RequestMapping("/api/listings/{id}")
class BiddingController(private val bidding: BiddingService) {
    @PostMapping("/bids")
    fun place(p: Principal,@PathVariable id: UUID,@RequestBody req: BidRequest) = bidding.place(UUID.fromString(p.name),id,req.rate,req.requestKey).toDto()
    @GetMapping("/bids")
    fun bids(p: Principal,@PathVariable id: UUID) = bidding.bids(UUID.fromString(p.name),id).map { it.toDto() }
    @PostMapping("/accept")
    fun accept(p: Principal,@PathVariable id: UUID,@RequestBody req: AwardRequest) = bidding.accept(UUID.fromString(p.name),id,req.bidId)
    @GetMapping("/auction")
    fun snapshot(p: Principal,@PathVariable id: UUID) = bidding.snapshot(UUID.fromString(p.name),id)
}
