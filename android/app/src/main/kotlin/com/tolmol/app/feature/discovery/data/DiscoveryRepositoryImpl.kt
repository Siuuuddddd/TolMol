package com.tolmol.app.feature.discovery.data
import com.tolmol.app.feature.discovery.domain.*
import com.tolmol.app.core.network.serverMessage
import com.tolmol.app.core.websocket.LiveSocket
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.math.BigDecimal

class DiscoveryRepositoryImpl(private val api: DiscoveryApi) : DiscoveryRepository {
    override suspend fun nearby() = request { api.nearby().map { it.toDomain() } }
    override suspend fun watched() = request { api.watched().map { it.toDomain() } }
    override suspend fun watch(id: String, enabled: Boolean) = request {
        if (enabled) api.watch(id) else api.unwatch(id)
    }

    private fun NearbyLotDto.toDomain() = NearbyLot(id, category, weight.content.toBigDecimal(), startingRate.content.toBigDecimal(), deadline, status)

    private suspend fun <T> request(action: suspend () -> T): T = try { action() }
    catch (error: CancellationException) { throw error }
    catch (error: Exception) { throw DiscoveryError.RequestFailed(error.serverMessage()) }
}
