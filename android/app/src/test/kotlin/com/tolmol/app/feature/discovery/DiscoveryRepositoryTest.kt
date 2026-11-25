package com.tolmol.app.feature.discovery

import com.tolmol.app.feature.discovery.data.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class DiscoveryRepositoryTest {
    @Test fun preservesDecimalRateAndDelegatesWatchActions() = runTest {
        val lot = NearbyLotDto("lot", "Paper", JsonPrimitive(BigDecimal("12.750")),
            JsonPrimitive(BigDecimal("17.45")), "2026-11-25T20:00:00Z", "OPEN")
        var watched = false
        val api = object : DiscoveryApi {
            override suspend fun detail(id: String) = lot
            override suspend fun nearby() = listOf(lot)
            override suspend fun watched() = if (watched) listOf(lot) else emptyList()
            override suspend fun watch(id: String) { assertEquals(lot.id, id); watched = true }
            override suspend fun unwatch(id: String) { assertEquals(lot.id, id); watched = false }
        }
        val repo = DiscoveryRepositoryImpl(api)
        assertEquals(BigDecimal("17.45"), repo.nearby().single().startingRate)
        repo.watch("lot", true)
        assertEquals("lot", repo.watched().single().id)
        repo.watch("lot", false)
        assertTrue(repo.watched().isEmpty())
    }
}
