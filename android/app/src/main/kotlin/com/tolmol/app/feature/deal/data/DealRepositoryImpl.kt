package com.tolmol.app.feature.deal.data
import com.tolmol.app.feature.deal.domain.*
import com.tolmol.app.core.network.serverMessage
import com.tolmol.app.core.websocket.LiveSocket
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.math.BigDecimal

class DealRepositoryImpl(private val api: DealApi, private val socket: LiveSocket) : DealRepository {
    override val live = socket.updates
    override suspend fun mine() = request { api.mine().map { it.toDomain() } }
    override suspend fun deal(id: String) = request { api.deal(id).toDomain() }
    override suspend fun inspect(id: String, weight: BigDecimal) = request { api.inspect(id, buildJsonObject { put("weight", JsonPrimitive(weight)) }).toDomain() }
    override suspend fun propose(id: String, amount: BigDecimal) = request { api.propose(id, buildJsonObject { put("amount", JsonPrimitive(amount)) }).toDomain() }
    override suspend fun confirm(id: String, revision: Long) = request { api.confirm(id, buildJsonObject { put("revision", revision) }).toDomain() }
    override suspend fun cancel(id: String) = request { api.cancel(id).toDomain() }

    private fun DealDto.toDomain() = Deal(listing, DealState.valueOf(state), buyer, seller, rate.content.toBigDecimal(),
        estimatedWeight.content.toBigDecimal(), actualWeight?.content?.toBigDecimal(), amount?.content?.toBigDecimal(),
        proposedBy, revision, relisted, pickupLatitude, pickupLongitude)

    private suspend fun <T> request(action: suspend () -> T): T = try { action() }
    catch (error: CancellationException) { throw error }
    catch (error: Exception) { throw DealError.RequestFailed(error.serverMessage()) }
}
