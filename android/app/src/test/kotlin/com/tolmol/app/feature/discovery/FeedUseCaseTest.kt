package com.tolmol.app.feature.discovery
import com.tolmol.app.feature.discovery.domain.*
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant

internal class FeedRepositoryFake : DiscoveryRepository {
    var lots: List<NearbyLot> = emptyList()
    var watchedIds: Set<String> = emptySet()
    var loadFails=false
    var watchGate: kotlinx.coroutines.CompletableDeferred<Unit>?=null
    override suspend fun detail(id: String)=lots.first { it.id==id }
    override suspend fun nearby(): List<NearbyLot> {
        if(loadFails) error("Connection unavailable")
        return lots
    }
    override suspend fun watched()=lots.filter { it.id in watchedIds }
    override suspend fun watch(id: String,enabled: Boolean) {
        watchGate?.await()
        watchedIds=if(enabled) watchedIds+id else watchedIds-id
    }
}
internal fun feedLot(id: String,category: String,deadline: Instant,title: String=id,description: String="") =
    NearbyLot(id,category,20.toBigDecimal(),14.toBigDecimal(),deadline.toString(),"OPEN",title,description,distanceKm=2.4)
class FeedUseCaseTest {
    private val now=Instant.parse("2026-11-26T08:00:00Z")
    private val feed=FeedUseCase(DiscoveryUseCase(FeedRepositoryFake()))
    private val lots=listOf(
        feedLot("metal","METAL",now.plusSeconds(3599),"Iron offcuts"),
        feedLot("paper","Paper",now.plusSeconds(7200),"Newspaper bundles","Office cleanout"),
        feedLot("card","CARDBOARD",now.plusSeconds(9000),"Cardboard cartons"),
        feedLot("expired","PAPER",now.minusSeconds(1)))
    @Test fun groupsCategoriesAndOnlyShowsActuallyEndingSoon() {
        val sections=feed.sections(lots,null,"",false,emptySet(),emptyList(),now)
        assertEquals(listOf("metal"),sections.first { it.id=="ending" }.lots.map {it.id})
        assertEquals(setOf("paper","card"),sections.first { it.id=="paper" }.lots.map {it.id}.toSet())
        assertFalse(sections.any {it.id=="recent"})
        assertFalse(sections.any { section -> section.lots.any {it.id=="expired"} })
    }
    @Test fun searchCategoryWatchAndRecentFiltersComposeWithoutInventingResults() {
        val search=feed.sections(lots,ScrapCategory.PAPER,"OFFICE",false,emptySet(),listOf("paper"),now)
        assertEquals(listOf("paper"),search.first {it.id=="recent"}.lots.map {it.id})
        val watched=feed.sections(lots,null,"",true,setOf("card"),emptyList(),now)
        assertTrue(watched.all {section -> section.lots.all {it.id=="card"}})
        assertTrue(feed.sections(lots,ScrapCategory.METAL,"newspaper",false,emptySet(),emptyList(),now).isEmpty())
    }
}
