package com.tolmol.marketplace.infra.redis

import org.springframework.data.geo.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import com.tolmol.marketplace.domain.model.Listing
import com.tolmol.user.domain.model.Position
import java.time.Duration
import java.util.UUID

@Component
class ListingGeoIndex(private val redis: StringRedisTemplate) {
    private val key = "tolmol:geo:listings"
    fun add(lot: Listing) {
        if (kotlin.math.abs(lot.latitude) > 85) return
        try {
            redis.opsForGeo().add(key, Point(lot.longitude,lot.latitude), lot.id.toString())
        } catch (_: org.springframework.dao.DataAccessException) { /* Discovery repairs missing entries. */ }
    }

    fun candidates(count: Long, p: Position, load: () -> List<Listing>): List<UUID>? {
        if (count == 0L || kotlin.math.abs(p.latitude) > 84) return null

        return try {
            if (redis.opsForZSet().zCard(key) != count) {
                val points = load().filter { kotlin.math.abs(it.latitude) <= 85 }.associate { it.id.toString() to Point(it.longitude,it.latitude) }
                if (points.isEmpty()) return null
                redis.opsForGeo().add(key,points)

            }
            // Include a margin for Redis spherical distance and coordinate quantization.
            val nearby = redis.opsForGeo().radius(key,Circle(Point(p.longitude,p.latitude),Distance(21.0,Metrics.KILOMETERS)))
                ?.content?.map { UUID.fromString(it.content.name) }
            nearby.takeIf { redis.opsForZSet().zCard(key) == count }
        } catch (e: org.springframework.dao.DataAccessException) { null }
    }
}
