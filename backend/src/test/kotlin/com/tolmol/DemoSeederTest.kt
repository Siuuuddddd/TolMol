package com.tolmol
import com.tolmol.marketplace.infra.demo.DemoSeeder
import com.tolmol.platform.infra.config.Databases
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.tolmol.user.domain.model.*
import com.tolmol.user.service.AuthService
import com.tolmol.marketplace.service.MarketplaceService
import java.util.UUID

@TestPropertySource(properties=["tolmol.seed.demo=true"])
class DemoSeederTest : BackendTest() {
    @Autowired lateinit var seed: DemoSeeder
    @Autowired lateinit var db: Databases
    @Autowired lateinit var auth: AuthService
    @Autowired lateinit var market: MarketplaceService
    @Autowired lateinit var environment: org.springframework.core.env.ConfigurableEnvironment
    @Test fun rerunningSeedCreatesNoDuplicatesAndFeedCanFindFiftyOpenLots() {
        seed.seed()
        val demoUsers=db.users.sql.sql("select id from accounts where email like '%@tolmol.demo'").query(UUID::class.java).list()
        fun counts() = listOf(
            db.users.sql.sql("select count(*) from accounts where email like '%@tolmol.demo'").query(Long::class.java).single(),
            db.marketplace.sql.sql("select count(*) from listings where seller in (:users)").param("users",demoUsers).query(Long::class.java).single())
        val first=counts()
        seed.seed()
        assertEquals(first,counts())
        assertEquals(20L,first[0])
        val buyer=db.users.sql.sql("select id from accounts where email='demo.user09@tolmol.demo'").query(UUID::class.java).single()
        assertEquals(53L,first[1])
        val demoOpen=db.marketplace.sql.sql("select id from listings where seller in (:users) and status='OPEN'").param("users",demoUsers).query(UUID::class.java).set()
        assertEquals(50,demoOpen.size)
        assertTrue(market.feedDetails(buyer).map { it.lot.id }.containsAll(demoOpen))
        environment.propertySources.addFirst(org.springframework.core.env.MapPropertySource("moved-demo-center",mapOf(
            "tolmol.seed.center-lat" to 18.5204,"tolmol.seed.center-lng" to 73.8567)))
        try {
            seed.seed()
            assertTrue(market.feedDetails(buyer).map { it.lot.id }.containsAll(demoOpen))
        } finally {
            environment.propertySources.remove("moved-demo-center")
            seed.seed()
        }
        assertEquals(3L,db.marketplace.sql.sql("select count(*) from deals where buyer=?").param(buyer).query(Long::class.java).single())
        val unrelated=auth.register("retained-${UUID.randomUUID()}@example.org","unrelated-password",setOf(Role.BUYER))
        seed.reset()
        assertEquals(setOf(Role.BUYER),auth.roles(unrelated))
        assertEquals(0L,db.users.sql.sql("select count(*) from accounts where email like '%@tolmol.demo'").query(Long::class.java).single())
        seed.seed()
    }
}
