
package com.tolmol.marketplace.domain.model
import java.util.UUID
data class ListingPhoto(val id: UUID, val listing: UUID, val position: Int, val size: Long,
    val storageKey: String, val isCover: Boolean)
