package com.tolmol
import com.tolmol.marketplace.domain.model.*
import com.tolmol.marketplace.service.MarketplaceService
import com.tolmol.user.service.AuthService
import com.tolmol.user.domain.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.util.UUID

abstract class MarketFixture : BackendTest() {
    @Autowired lateinit var auth: AuthService
    @Autowired lateinit var market: MarketplaceService
    fun user(role: Role, lat: Double = 19.0): UUID {
        val id = auth.register("${UUID.randomUUID()}@example.org","a-test-password",setOf(role))
        auth.locate(id,role,Position(lat,72.0))
        return id
    }
    fun lot(seller: UUID) = market.publish(seller,LotDraft("Paper",20.toBigDecimal(),12.toBigDecimal(),Instant.now().plusSeconds(3600)))
}
class MarketplaceTest : MarketFixture() {
    @Autowired lateinit var redis: org.springframework.data.redis.core.StringRedisTemplate
    @Autowired lateinit var activity: com.tolmol.user.infra.database.repositories.ActivityProjection
    @Autowired lateinit var databases: com.tolmol.platform.infra.config.Databases

    @Test fun radiusCacheRecoveryAndRoleActivity() {
        val seller = user(Role.SELLER)
        val id = lot(seller).id
        val buyer = user(Role.BUYER,19.179)
        assertTrue(market.feed(buyer).any { it.id==id })
        redis.delete("tolmol:geo:listings")
        assertTrue(market.feed(buyer).any { it.id==id })
        assertTrue(redis.hasKey("tolmol:geo:listings"))
        assertTrue(redis.getExpire("tolmol:location:$buyer:BUYER:1") in 1..900)
        market.watch(buyer,id,true)
        activity.project()
        assertEquals(1L,databases.users.sql.sql("select count(*) from seller.activity where user_id=? and listing_id=?").params(seller,id).query(Long::class.java).single())
        assertEquals(1L,databases.users.sql.sql("select count(*) from buyer.activity where user_id=? and listing_id=?").params(buyer,id).query(Long::class.java).single())
        auth.locate(buyer,Role.BUYER,Position(19.181,72.0))
        assertFalse(market.feed(buyer).any { it.id==id })
    }
    @Test fun discoveryAndWatchEligibility() {
        val id = lot(user(Role.SELLER)).id
        val buyer = user(Role.BUYER)
        assertTrue(market.feed(buyer).any { it.id==id })
        market.watch(buyer,id,true)
        market.watch(buyer,id,true)
        assertEquals(listOf(id),market.watchlist(buyer).map { it.id })
        market.watch(buyer,id,false)
        assertTrue(market.watchlist(buyer).isEmpty())
        market.watch(buyer,id,true)
        auth.locate(buyer,Role.BUYER,Position(20.0,72.0))
        assertFalse(market.feed(buyer).any { it.id==id })
        assertTrue(market.watchlist(buyer).isEmpty())
        assertThrows(com.tolmol.marketplace.domain.exception.ListingUnavailableException::class.java) { market.watch(buyer,id,true) }
    }
}
