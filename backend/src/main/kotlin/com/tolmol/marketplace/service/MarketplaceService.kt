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
        require(draft.title == null || draft.title.trim().length in 1..160) { "Title must be 1–160 characters" }
        require(draft.description == null || draft.description.length <= 2000) { "Description is too long" }
        require(draft.imageKey == null || draft.imageKey.matches(Regex("[a-z0-9_]{1,80}"))) { "Invalid image key" }
        return Listing(UUID.randomUUID(),seller,draft.category,draft.weight,draft.startingRate,p.latitude,p.longitude,draft.deadline,"OPEN",0,draft.title?.trim(),draft.description?.trim(),draft.imageKey).also(store::insert)
    }
    fun describe(user: UUID,id: UUID): ListingInfo {
        val lot = listing(id)
        val distance = if (lot.seller == user) null else eligible(user,id).distanceKm(users.position(user,Role.BUYER))
        val seller = users.display(lot.seller)
        return ListingInfo(lot,distance,seller.first,seller.second)
    }
    fun feedDetails(user: UUID, watched: Boolean = false): List<ListingInfo> {
        val position = users.position(user,Role.BUYER)
        val ids = if (watched) store.watchedIds(user) else null
        val profiles = mutableMapOf<UUID,Pair<String,Instant?>>()
        return store.nearby(position).filter { it.seller != user && (ids == null || it.id in ids) }.map {
            val profile = profiles.getOrPut(it.seller) { users.display(it.seller) }
            ListingInfo(it,it.distanceKm(position),profile.first,profile.second)
        }
    }
    fun mine(user: UUID,after: UUID?,limit: Int): List<Listing> {
        require(limit in 1..100) { "Limit must be 1–100" }
        return store.mine(user,after,limit)
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

data class ListingInfo(val lot: Listing,val distanceKm: Double?,val sellerName: String,val memberSince: Instant?)
