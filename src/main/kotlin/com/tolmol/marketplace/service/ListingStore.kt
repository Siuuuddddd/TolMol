package com.tolmol.marketplace.service

import com.tolmol.marketplace.domain.model.*
import com.tolmol.user.domain.model.Position
import java.util.UUID
interface ListingStore {
    fun insert(listing: Listing)
    fun get(id: UUID): Listing?
    fun nearby(position: Position): List<Listing>
    fun watchedIds(buyer: UUID): Set<UUID>
    fun watch(id: UUID,buyer: UUID,enabled: Boolean)
}
