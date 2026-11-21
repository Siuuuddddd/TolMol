package com.tolmol.marketplace.domain.model

import java.util.UUID
import java.time.Instant
import java.math.BigDecimal
import com.tolmol.user.domain.model.Position
import kotlin.math.*

data class Listing(val id: UUID,val seller: UUID,val category: String,val weight: BigDecimal,val startingRate: BigDecimal,
    val latitude: Double,val longitude: Double,val deadline: Instant,val status: String,val version: Long) {
    fun distanceKm(p: Position): Double {
        val radians = Math.PI / 180
        val a = sin((latitude-p.latitude)*radians/2).pow(2) +
            cos(latitude*radians)*cos(p.latitude*radians)*sin((longitude-p.longitude)*radians/2).pow(2)
        return 12742 * asin(sqrt(a.coerceIn(0.0,1.0)))
    }
}
data class LotDraft(val category: String,val weight: BigDecimal,val startingRate: BigDecimal,val deadline: Instant)
