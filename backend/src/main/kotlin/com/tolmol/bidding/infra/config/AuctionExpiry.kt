package com.tolmol.bidding.infra.config

import com.tolmol.bidding.service.BiddingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import java.util.concurrent.atomic.AtomicBoolean
@Component
class AuctionExpiry(private val bidding: BiddingService) {
    private val ready = AtomicBoolean(false)
    @EventListener(ApplicationReadyEvent::class)
    fun applicationReady() { ready.set(true) }
    @Scheduled(fixedDelay=1000) fun closeDueAuctions() {
        // Runners must finish restoring demo deadlines before expiry can evaluate them.
        if (ready.get()) bidding.expire()
    }
}
