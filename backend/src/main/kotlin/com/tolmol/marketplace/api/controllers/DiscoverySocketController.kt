package com.tolmol.marketplace.api.controllers
import com.fasterxml.jackson.databind.JsonNode
import com.tolmol.platform.service.SocketCommand
import com.tolmol.marketplace.service.MarketplaceService
import com.tolmol.marketplace.api.mappers.toDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DiscoverySocketController(private val market: MarketplaceService) : SocketCommand {
    override val type = "nearby.subscribe"
    override fun handle(user: UUID,body: JsonNode) = market.feed(user).map { it.toDto() }
}
