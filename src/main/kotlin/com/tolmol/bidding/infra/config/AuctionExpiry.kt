package com.tolmol.bidding.infra.config

import com.tolmol.bidding.service.BiddingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
@Component
class AuctionExpiry(private val bidding: BiddingService) {
    @Scheduled(fixedDelay=1000) fun closeDueAuctions() = bidding.expire()
}
