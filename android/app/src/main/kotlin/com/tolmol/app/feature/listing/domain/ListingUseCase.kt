package com.tolmol.app.feature.listing.domain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import com.tolmol.app.core.common.LiveUpdate

data class Listing(val id: String, val category: String, val weight: BigDecimal, val startingRate: BigDecimal, val deadline: String, val status: String,val title: String? = null,val description: String? = null,val imageKey: String? = null)
data class Offer(val id: String, val buyer: String, val rate: BigDecimal, val createdAt: String)

sealed class ListingError(message: String) : RuntimeException(message) {
    class RequestFailed(message: String) : ListingError(message)
}

interface ListingRepository {
    suspend fun mine(after: String? = null): List<Listing>
    suspend fun publish(category: String, weight: BigDecimal, startingRate: BigDecimal, deadline: String,title: String? = null,description: String? = null): Listing
    suspend fun offers(id: String): List<Offer>
    suspend fun accept(id: String, bidId: String)
}

class ListingUseCase(repository: ListingRepository) : ListingRepository by repository
