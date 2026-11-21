package com.tolmol.chat.service

import com.tolmol.chat.domain.model.*
import com.tolmol.chat.domain.exception.*
import com.tolmol.marketplace.service.MarketplaceService
import com.tolmol.bidding.service.BiddingService
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ChatService(private val store: ChatStore,private val market: MarketplaceService,private val bidding: BiddingService) {
    fun open(buyer: UUID,listing: UUID): Conversation {
        val lot = market.listing(listing)
        if (lot.status=="OPEN") {
            market.eligible(buyer,listing)
            if (lot.deadline<=Instant.now()) throw ChatClosedException()
        } else if (lot.status!="AWARDED" || bidding.winningBuyer(listing)!=buyer) throw ChatClosedException()
        return store.open(listing,lot.seller,buyer)
    }
    private fun participant(user: UUID,id: UUID): Conversation {
        val thread = store.thread(id)
        if (user!=thread.seller && user!=thread.buyer) throw ConversationUnavailableException()
        return thread
    }
    fun send(user: UUID,id: UUID,body: String,clientId: UUID): Message {
        require(body.isNotBlank() && body.length<=4000) { "Message must contain 1–4000 characters" }
        val thread = participant(user,id)
        val lot = market.listing(thread.listing)
        val writable = when (lot.status) {
            "OPEN" -> Instant.now()<lot.deadline
            "AWARDED" -> bidding.winningBuyer(lot.id)==thread.buyer
            else -> false
        }
        if (!writable) throw ChatClosedException()
        return store.send(thread,user,body.trim(),clientId)
    }
    fun history(user: UUID,id: UUID,after: Long?): List<Message> {
        participant(user,id)
        require(after==null || after>=0)
        return store.history(id,after)
    }
}
