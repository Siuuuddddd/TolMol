
package com.tolmol.marketplace.api.controllers
import com.tolmol.marketplace.api.dto.ListingDto
import com.tolmol.marketplace.service.ListingPhotoService
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import org.springframework.core.MethodParameter
import org.springframework.http.*
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.*
@RestControllerAdvice(basePackages=["com.tolmol.marketplace.api.controllers"])
class ListingPhotoResponse(private val photos: ListingPhotoService) : ResponseBodyAdvice<Any> {
    override fun supports(p: MethodParameter,c: Class<out HttpMessageConverter<*>>) = true
    override fun beforeBodyWrite(body: Any?,p: MethodParameter,t: MediaType,c: Class<out HttpMessageConverter<*>>,req: ServerHttpRequest,res: ServerHttpResponse): Any? {
        fun enrich(lot: ListingDto): ListingDto {
            val urls=photos.list(lot.id).map { "/api/listings/${lot.id}/photos/${it.id}" }
            return lot.copy(coverPhotoUrl=urls.firstOrNull(),photoUrls=urls)
        }
        return when(body) { is ListingDto -> enrich(body); is List<*> -> body.map { if(it is ListingDto) enrich(it) else it }; else -> body }
    }
}
