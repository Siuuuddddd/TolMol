
package com.tolmol
import com.tolmol.marketplace.service.ListingPhotoService
import com.tolmol.marketplace.infra.photos.PhotoSanitizer
import com.tolmol.user.domain.model.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.server.ResponseStatusException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
class ListingPhotoTest : MarketFixture() {
    @Autowired lateinit var photos: ListingPhotoService
    @Autowired lateinit var http: org.springframework.boot.test.web.client.TestRestTemplate
    @Autowired lateinit var issuer: com.tolmol.user.service.TokenIssuer
    @Test fun authenticatedMultipartEndpointAndUrls() {
        val seller=user(Role.SELLER);val id=lot(seller).id
        val headers=org.springframework.http.HttpHeaders().apply {
            setBearerAuth(issuer.issue(seller));set("Idempotency-Key","http-photo")
            contentType=org.springframework.http.MediaType.MULTIPART_FORM_DATA
        }
        val partHeaders=org.springframework.http.HttpHeaders().apply {contentType=org.springframework.http.MediaType.IMAGE_JPEG}
        val file=object: org.springframework.core.io.ByteArrayResource(jpeg()) {override fun getFilename()="photo.jpg"}
        val body=org.springframework.util.LinkedMultiValueMap<String,Any>()
        body.add("files",org.springframework.http.HttpEntity(file,partHeaders))
        val response=http.postForEntity("/api/listings/$id/photos",org.springframework.http.HttpEntity(body,headers),String::class.java)
        assertEquals(200,response.statusCode.value())
        val photo=photos.list(id).single()
        val buyerHeaders=org.springframework.http.HttpHeaders().apply {setBearerAuth(issuer.issue(user(Role.BUYER)))}
        val read=http.exchange("/api/listings/$id/photos/${photo.id}",org.springframework.http.HttpMethod.GET,
            org.springframework.http.HttpEntity<Void>(buyerHeaders),ByteArray::class.java)
        assertEquals(200,read.statusCode.value())
        assertEquals(org.springframework.http.MediaType.IMAGE_JPEG,read.headers.contentType)
        assertEquals(401,http.getForEntity("/api/listings/$id/photos/${photo.id}",String::class.java).statusCode.value())
        val listing=http.exchange("/api/listings/$id",org.springframework.http.HttpMethod.GET,
            org.springframework.http.HttpEntity<Void>(buyerHeaders),String::class.java).body.orEmpty()
        assertTrue(listing.contains("coverPhotoUrl"));assertTrue(listing.contains(photo.id.toString()))
    }
    private fun jpeg() = ByteArrayOutputStream().use { out -> ImageIO.write(BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"jpeg",out);out.toByteArray() }
    @Test fun ownerUploadsBuyerReadsAndRetryIsIdempotent() {
        val seller=user(Role.SELLER);val id=lot(seller).id;val bytes=jpeg()
        val first=photos.upload(seller,id,"stable-key",bytes,"image/jpeg")
        assertEquals(first,photos.upload(seller,id,"stable-key",bytes,"image/jpeg"))
        assertNotNull(ImageIO.read(photos.read(user(Role.BUYER),id,first.single().id).inputStream()))
        assertThrows(ResponseStatusException::class.java) {photos.upload(user(Role.SELLER),id,"other",bytes,"image/jpeg")}
        assertThrows(com.tolmol.marketplace.domain.exception.ListingUnavailableException::class.java) {photos.read(user(Role.BUYER,25.0),id,first.single().id)}
    }
    @Test fun validatesTypeSizeAndPhotoLimit() {
        val seller=user(Role.SELLER);val id=lot(seller).id
        assertThrows(IllegalArgumentException::class.java) {photos.upload(seller,id,"bad","not an image".toByteArray(),"image/jpeg")}
        assertThrows(IllegalArgumentException::class.java) {photos.upload(seller,id,"oversize",ByteArray(5*1024*1024+1),"image/jpeg")}
        assertThrows(IllegalArgumentException::class.java) {photos.upload(seller,id,"spoof",jpeg(),"image/png")}
        repeat(5) {photos.upload(seller,id,"photo-$it",jpeg(),"image/jpeg")}
        assertThrows(IllegalArgumentException::class.java) {photos.upload(seller,id,"sixth",jpeg(),"image/jpeg")}
        val list=photos.list(id)
        photos.cover(seller,id,list.last().id)
        assertEquals(list.last().id,photos.list(id).first().id)
        photos.delete(seller,id,list.last().id)
        assertEquals(4,photos.list(id).size)
        assertTrue(photos.list(id).first().isCover)
    }
    @Test fun stripsExifGpsMetadata() {
        // JPEG APP1 carrying EXIF GPS IFD data is omitted by the fresh RGB re-encode.
        val original=jpeg()
        val exif=byteArrayOf(69,120,105,102,0,0,73,73,42,0,8,0,0,0,1,0,37,-120,4,0,1,0,0,0,26,0,0,0,0,0,0,0,0,0)
        val length=exif.size+2
        val input=original.take(2).toByteArray()+byteArrayOf(-1,-31,(length shr 8).toByte(),length.toByte())+exif+original.drop(2).toByteArray()
        val output=PhotoSanitizer().clean(input,"image/jpeg")
        assertFalse(String(output,Charsets.ISO_8859_1).contains("Exif"))
        assertNotNull(ImageIO.read(output.inputStream()))
    }
}

