package com.tolmol.app.feature.discovery.domain
import java.time.Instant
import java.time.Duration

enum class ScrapCategory(val label: String) {
    METAL("Metal"),PAPER("Paper"),CARDBOARD("Cardboard"),PLASTIC("Plastic"),PAINT("Paint"),
    ELECTRONICS("Electronics"),FURNITURE("Furniture"),OTHER("Other");
    companion object {
        fun of(raw: String): ScrapCategory {
            val value=raw.uppercase()
            return entries.firstOrNull { it.name==value } ?: when {
                "PAPER" in value || "NEWSPAPER" in value -> PAPER
                "CARD" in value -> CARDBOARD
                "PAINT" in value -> PAINT
                "PLASTIC" in value -> PLASTIC
                "METAL" in value || "IRON" in value || "COPPER" in value -> METAL
                "ELECTR" in value || "APPLIANCE" in value -> ELECTRONICS
                "FURNITURE" in value -> FURNITURE
                else -> OTHER
            }
        }
    }
}
data class FeedSection(val id: String,val title: String,val lots: List<NearbyLot>)
data class FeedSnapshot(val lots: List<NearbyLot>,val watched: Set<String>)
class FeedUseCase(private val discovery: DiscoveryUseCase) {
    suspend fun load() = FeedSnapshot(discovery.nearby(),discovery.watched().map { it.id }.toSet())
    suspend fun watch(id: String,enabled: Boolean) = discovery.watch(id,enabled)
    fun sections(lots: List<NearbyLot>, category: ScrapCategory?, query: String, watchedOnly: Boolean,
                 watched: Set<String>, recent: List<String>, now: Instant): List<FeedSection> {
        val text=query.trim()
        val filtered=lots.filter {
            it.status=="OPEN" && runCatching { Instant.parse(it.deadline)>now }.getOrDefault(false) &&
            (category==null || ScrapCategory.of(it.category)==category) && (!watchedOnly || it.id in watched) &&
            (text.isEmpty() || listOf(it.title,it.category,it.description).any { field -> field.contains(text,true) })
        }
        val groups=listOf(
            FeedSection("ending","Ending soon",filtered.filter { endingSoon(it,now) }.sortedBy { it.deadline }),
            FeedSection("recent","Recently viewed",recent.mapNotNull { id -> filtered.find { it.id==id } }),
            FeedSection("nearby",if(watchedOnly) "Your watchlist" else "Near you",filtered.sortedBy { it.distanceKm ?: Double.MAX_VALUE }),
            FeedSection("metal","Metal",filtered.filter { ScrapCategory.of(it.category)==ScrapCategory.METAL }),
            FeedSection("paper","Paper & cardboard",filtered.filter { ScrapCategory.of(it.category) in setOf(ScrapCategory.PAPER,ScrapCategory.CARDBOARD) }),
            FeedSection("plastic","Plastic",filtered.filter { ScrapCategory.of(it.category)==ScrapCategory.PLASTIC }),
            FeedSection("paint","Paint",filtered.filter { ScrapCategory.of(it.category)==ScrapCategory.PAINT }),
            FeedSection("electronics","Used electronics",filtered.filter { ScrapCategory.of(it.category)==ScrapCategory.ELECTRONICS }),
            FeedSection("furniture","Used furniture",filtered.filter { ScrapCategory.of(it.category)==ScrapCategory.FURNITURE }),
            FeedSection("other","Other finds",filtered.filter { ScrapCategory.of(it.category)==ScrapCategory.OTHER }))
        return groups.filter { it.lots.isNotEmpty() }
    }
}
fun endingSoon(lot: NearbyLot,now: Instant) = runCatching {
    val seconds=Duration.between(now,Instant.parse(lot.deadline)).seconds
    seconds in 1 until 7200
}.getOrDefault(false)
