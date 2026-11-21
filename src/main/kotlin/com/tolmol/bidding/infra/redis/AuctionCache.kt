package com.tolmol.bidding.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.tolmol.bidding.domain.model.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AuctionCache(private val redis: StringRedisTemplate,private val json: ObjectMapper) {
    fun read(auction: Auction,load: ()->Snapshot): Snapshot {
        val key = "tolmol:auction:${auction.listing}:${auction.version}"
        try {
            redis.opsForValue().get(key)?.let { return json.readValue(it,Snapshot::class.java) }
        } catch (_: DataAccessException) { return load() }
        val snapshot = load()
        try { redis.opsForValue().set(key,json.writeValueAsString(snapshot),Duration.ofMinutes(15)) }
        catch (_: DataAccessException) { /* The database snapshot is current. */ }
        return snapshot
    }
}
