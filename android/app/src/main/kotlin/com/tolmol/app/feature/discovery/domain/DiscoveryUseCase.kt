package com.tolmol.app.feature.discovery.domain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import com.tolmol.app.core.common.LiveUpdate

data class NearbyLot(val id: String, val category: String, val weight: BigDecimal, val startingRate: BigDecimal, val deadline: String, val status: String)

sealed class DiscoveryError(message: String) : RuntimeException(message) {
    class RequestFailed(message: String) : DiscoveryError(message)
}

interface DiscoveryRepository {
    suspend fun nearby(): List<NearbyLot>
    suspend fun watched(): List<NearbyLot>
    suspend fun watch(id: String, enabled: Boolean)
}

class DiscoveryUseCase(repository: DiscoveryRepository) : DiscoveryRepository by repository
