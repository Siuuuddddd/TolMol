
package com.tolmol.marketplace.service
import com.tolmol.marketplace.domain.model.ListingPhoto
import java.util.UUID
interface PhotoStore {
    fun participant(listing: UUID,user: UUID): Boolean
    fun list(listing: UUID): List<ListingPhoto>
    fun <T> edit(listing: UUID, action: () -> T): T
    fun findRequest(listing: UUID, key: String): ListingPhoto?
    fun insert(photo: ListingPhoto, request: String)
    fun remove(listing: UUID, photo: UUID)
    fun cover(listing: UUID, photo: UUID)
}
