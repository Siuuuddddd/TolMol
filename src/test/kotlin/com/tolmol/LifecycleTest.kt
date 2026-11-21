package com.tolmol

import com.fasterxml.jackson.databind.JsonNode
import com.tolmol.user.domain.model.Role
import com.tolmol.bidding.domain.exception.*
import com.tolmol.platform.infra.config.Databases
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID
import java.util.concurrent.*

class LifecycleTest : SocketFixture() {
    @Autowired lateinit var db: Databases
    @Test fun concurrentSubscribeAndBidThenUnsubscribe() {
        val viewer = user(Role.BUYER)
        val id = lot(user(Role.SELLER)).id
        val buyer = user(Role.BUYER)
        val inbox = LinkedBlockingQueue<JsonNode>()
        val socket = connect(viewer,inbox)
        fun command(type: String) = socket.sendText(json.writeValueAsString(mapOf("type" to type,"listingId" to id)),true).join()
        fun until(predicate: (JsonNode)->Boolean): JsonNode {
            val end = System.nanoTime()+TimeUnit.SECONDS.toNanos(12)
            while (System.nanoTime()<end) {
                val event = inbox.poll(200,TimeUnit.MILLISECONDS)
                if (event!=null && predicate(event)) return event
            }
            error("Missing socket event")
        }
        try {
            val placing = CompletableFuture.runAsync { bid(buyer,id,12) }
            command("auction.subscribe")
            val ack = until { it["type"].asText()=="ack" }
            placing.join()
            if (ack["data"]["version"].asLong()==0L) until { it.path("data").path("version").asLong()==1L }
            command("auction.unsubscribe")
            until { it["type"].asText()=="ack" }
            val next = user(Role.BUYER)
            bid(next,id,13)
            market.watch(viewer,id,true)
            bid(user(Role.BUYER),id,14)
            until {
                assertFalse(it.path("data").path("version").asLong()==2L)
                it.path("data").path("version").asLong()==3L
            }
        } finally { socket.abort() }
    }
    @Test fun simultaneousBidsAndExpiryProduceOneWinner() {
        val seller = user(Role.SELLER)
        val id = lot(seller).id
        val buyers = List(2) { user(Role.BUYER) }
        val barrier = CyclicBarrier(2)
        val attempts = buyers.map { user -> CompletableFuture.supplyAsync {
            barrier.await()
            try { bid(user,id,15) } catch (_: InvalidBidException) { null }
        } }.mapNotNull { it.join() }
        val only = attempts.single()
        db.marketplace.sql.sql("update listings set deadline=now() where id=?").param(id).update()
        val race = CyclicBarrier(2)
        val expiry = CompletableFuture.runAsync { race.await(); bidding.expire() }
        race.await()
        assertThrows(AuctionClosedException::class.java) { bidding.accept(seller,id,only.id) }
        expiry.join()
        assertEquals(only.buyer,bidding.winningBuyer(id))
        assertEquals(1L,db.marketplace.sql.sql("select count(*) from outbox where aggregate_id=? and event_type='AUCTION_AWARDED'").param(id).query(Long::class.java).single())
    }
}
