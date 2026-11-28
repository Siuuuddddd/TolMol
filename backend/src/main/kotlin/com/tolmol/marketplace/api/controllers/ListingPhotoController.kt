
package com.tolmol.marketplace.api.controllers
import com.tolmol.marketplace.service.ListingPhotoService
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.*
import java.security.Principal
import java.util.UUID
@RestController
@RequestMapping("/api/listings/{id}/photos")
class ListingPhotoController(private val photos: ListingPhotoService) {
    data class CoverRequest(val photoId: UUID)
    @PostMapping(consumes=[MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(p: Principal,@PathVariable id: UUID,@RequestHeader("Idempotency-Key") key: String,
        @RequestPart("files") files: List<MultipartFile>): List<Map<String,Any>> {
        require(files.size in 1..5) { "Choose 1 to 5 photos" }
        files.forEachIndexed { index,file -> photos.upload(UUID.fromString(p.name),id,"$key:$index",file.bytes,file.contentType.orEmpty()) }
        return photos.list(id).map { mapOf("id" to it.id,"url" to "/api/listings/$id/photos/${it.id}","isCover" to it.isCover) }
    }
    @DeleteMapping("/{photo}") fun delete(p: Principal,@PathVariable id: UUID,@PathVariable photo: UUID) = photos.delete(UUID.fromString(p.name),id,photo)
    @PutMapping("/cover") fun cover(p: Principal,@PathVariable id: UUID,@RequestBody body: CoverRequest) = photos.cover(UUID.fromString(p.name),id,body.photoId)
    @GetMapping("/{photo}") fun read(p: Principal,@PathVariable id: UUID,@PathVariable photo: UUID): ResponseEntity<ByteArray> =
        ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).header("Cache-Control","private, max-age=3600")
            .body(photos.read(UUID.fromString(p.name),id,photo))
}
