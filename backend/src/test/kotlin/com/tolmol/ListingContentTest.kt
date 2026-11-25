package com.tolmol
import com.tolmol.marketplace.domain.model.LotDraft
import com.tolmol.marketplace.api.mappers.toDto
import com.tolmol.user.domain.model.Role
import com.tolmol.user.domain.model.Position
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class ListingContentTest : MarketFixture() {
    @Test fun contentSurvivesPublicationAndPublicDetailsNeverExposePickupCoordinates() {
        val seller=user(Role.SELLER)
        val buyer=user(Role.BUYER,19.01)
        val lot=market.publish(seller,LotDraft("METAL",25.toBigDecimal(),18.toBigDecimal(),Instant.now().plusSeconds(3600),
            "Rusty mixed iron","Sorted rusty iron fittings.","scrap_metal_01"))
        val detail=market.describe(buyer,lot.id).toDto()
        assertEquals("Rusty mixed iron",detail.title)
        assertEquals("scrap_metal_01",detail.imageKey)
        assertTrue(detail.distanceKm!! in 0.5..2.0)
        assertTrue(detail.distanceKm!!.toBigDecimal().stripTrailingZeros().scale() <= 1,
            "Public distance must not reveal precise pickup location")
        assertEquals(detail,market.feedDetails(buyer).first { it.lot.id==lot.id }.toDto())
        val fields=detail.javaClass.declaredFields.map { it.name }
        assertFalse("latitude" in fields)
        assertFalse("longitude" in fields)
        auth.locate(buyer,Role.BUYER,Position(22.0,72.0))
        assertThrows(com.tolmol.marketplace.domain.exception.ListingUnavailableException::class.java) { market.describe(buyer,lot.id) }
    }
}
