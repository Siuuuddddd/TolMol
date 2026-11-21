package com.tolmol.marketplace.service

import com.tolmol.marketplace.domain.model.*
import com.tolmol.marketplace.domain.exception.*
import com.tolmol.user.service.AuthService
import com.tolmol.user.domain.model.Role
import org.springframework.stereotype.Service
import java.util.UUID
import java.time.Instant

@Service
class MarketplaceService(private val store: ListingStore,private val users: AuthService) {
    fun publish(seller: UUID,draft: LotDraft): Listing {
        val p = users.position(seller,Role.SELLER)
        require(draft.category.isNotBlank() && draft.category.length <= 100 && draft.weight.signum()>0 &&
            draft.weight.scale()<=3 && draft.startingRate.signum()>0 && draft.startingRate.scale()<=2 && draft.deadline>Instant.now()) { "Invalid lot details" }
        return Listing(UUID.randomUUID(),seller,draft.category,draft.weight,draft.startingRate,p.latitude,p.longitude,draft.deadline,"OPEN",0).also(store::insert)
    }
    fun listing(id: UUID) = store.get(id) ?: throw ListingUnavailableException()
    fun eligible(buyer: UUID,id: UUID): Listing {
        val lot = listing(id)
        if (lot.seller == buyer || lot.distanceKm(users.position(buyer,Role.BUYER))>20) throw ListingUnavailableException()
        return lot
    }
    fun feed(buyer: UUID) = store.nearby(users.position(buyer,Role.BUYER)).filter { it.seller != buyer }
    fun watchlist(buyer: UUID): List<Listing> {
        val ids = store.watchedIds(buyer)
        return feed(buyer).filter { it.id in ids }
    }
    fun watch(buyer: UUID,id: UUID,enabled: Boolean) {
        if (enabled) {
            val lot = eligible(buyer,id)
            require(lot.status=="OPEN" && lot.deadline>Instant.now()) { "Auction is closed" }
        }
        store.watch(id,buyer,enabled)
    }
}
