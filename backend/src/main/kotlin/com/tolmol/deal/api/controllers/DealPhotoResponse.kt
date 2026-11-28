
package com.tolmol.deal.api.controllers
import com.tolmol.deal.api.dto.DealDto
import com.tolmol.marketplace.service.ListingPhotoService
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import org.springframework.core.MethodParameter
import org.springframework.http.*
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.*
@RestControllerAdvice(basePackages=["com.tolmol.deal.api.controllers"])
class DealPhotoResponse(private val photos: ListingPhotoService,private val market: com.tolmol.marketplace.service.MarketplaceService) : ResponseBodyAdvice<Any> {
    override fun supports(p: MethodParameter,c: Class<out HttpMessageConverter<*>>) = true
    override fun beforeBodyWrite(body: Any?,p: MethodParameter,t: MediaType,c: Class<out HttpMessageConverter<*>>,req: ServerHttpRequest,res: ServerHttpResponse): Any? {
        fun enrich(lot: DealDto): DealDto {
            val urls=photos.list(lot.listing).map { "/api/listings/${lot.listing}/photos/${it.id}" }
            val item=market.listing(lot.listing)
            return lot.copy(coverPhotoUrl=urls.firstOrNull(),photoUrls=urls,imageKey=item.imageKey,category=item.category)
        }
        return when(body) { is DealDto -> enrich(body); is List<*> -> body.map { if(it is DealDto) enrich(it) else it }; else -> body }
    }
}
