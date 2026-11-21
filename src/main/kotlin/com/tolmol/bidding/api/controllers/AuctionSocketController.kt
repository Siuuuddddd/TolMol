package com.tolmol.bidding.api.controllers

import com.tolmol.bidding.service.BiddingService
import com.tolmol.platform.service.SocketCommand
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component
import java.util.UUID
@Component
class AuctionSocketController(private val bidding: BiddingService) : SocketCommand {
    override val type = "auction.subscribe"
    override fun handle(user: UUID,body: JsonNode) = bidding.snapshot(user,UUID.fromString(body.path("listingId").asText()))
}
