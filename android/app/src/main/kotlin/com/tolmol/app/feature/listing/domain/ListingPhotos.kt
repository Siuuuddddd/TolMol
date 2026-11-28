
package com.tolmol.app.feature.listing.domain

data class PreparedPhoto(val key: String,val source: String,val bytes: ByteArray)
interface ListingPhotoRepository {
    suspend fun prepare(source: String): PreparedPhoto
    suspend fun upload(listing: String,photo: PreparedPhoto)
}
class UploadListingPhotosUseCase(private val repository: ListingPhotoRepository) {
    suspend fun prepare(source: String) = repository.prepare(source)
    suspend fun upload(listing: String,photo: PreparedPhoto) = repository.upload(listing,photo)
}
