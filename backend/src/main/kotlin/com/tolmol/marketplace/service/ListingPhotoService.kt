
package com.tolmol.marketplace.service
import com.tolmol.marketplace.domain.model.ListingPhoto
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID
@Service
class ListingPhotoService(private val market: MarketplaceService,private val store: PhotoStore,
    private val storage: PhotoStorage,private val sanitizer: PhotoProcessor) {
    fun list(id: UUID) = store.list(id)
    private fun owner(user: UUID,id: UUID) {
        val lot=market.listing(id)
        if(lot.seller!=user) throw ResponseStatusException(HttpStatus.FORBIDDEN,"Only the seller can change photos")
        require(lot.status=="OPEN" && lot.deadline>Instant.now()) { "Listing is closed" }
    }
    fun upload(user: UUID,id: UUID,key: String,bytes: ByteArray,type: String): List<ListingPhoto> {
        require(key.length in 1..100) { "Provide a stable upload key" }
        owner(user,id)
        val clean=try { sanitizer.clean(bytes,type) } catch(e: java.io.IOException) { throw IllegalArgumentException("Unable to decode photo",e) }
        var created: String?=null
        try {
            return store.edit(id) {
                owner(user,id)
                if(store.findRequest(id,key)==null) {
                    val current=store.list(id)
                    require(current.size<5) { "A listing can have at most 5 photos" }
                    val storageKey=storage.save(clean); created=storageKey
                    val position=(0..4).first { index -> current.none { it.position==index } }
                    store.insert(ListingPhoto(UUID.randomUUID(),id,position,clean.size.toLong(),storageKey,current.isEmpty()),key)
                }
                store.list(id)
            }
        } catch(e: Exception) { created?.let(storage::delete); throw e }
    }
    fun delete(user: UUID,id: UUID,photo: UUID) = store.edit(id) { owner(user,id); store.remove(id,photo) }
    fun cover(user: UUID,id: UUID,photo: UUID) = store.edit(id) { owner(user,id); store.cover(id,photo) }
    fun read(user: UUID,id: UUID,photo: UUID): ByteArray {
        if(!store.participant(id,user)) market.describe(user,id)
        val p=store.list(id).find { it.id==photo } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return storage.read(p.storageKey)
    }
}
