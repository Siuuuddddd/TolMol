package com.tolmol.app.feature.discovery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tolmol.app.feature.discovery.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class DiscoveryUiState(
    val lots: List<NearbyLot> = emptyList(),val watched: Set<String> = emptySet(),
    val loading: Boolean = false,val loaded: Boolean = false,val error: String? = null,
    val query: String = "",val category: ScrapCategory? = null,val watchedOnly: Boolean = false,
    val recent: List<String> = emptyList(),val section: String? = null,val pending: Set<String> = emptySet(),
)
sealed interface DiscoveryEvent {
    data object Refresh : DiscoveryEvent
    data class Query(val value: String) : DiscoveryEvent
    data class Category(val value: ScrapCategory?) : DiscoveryEvent
    data object WatchedOnly : DiscoveryEvent
    data class Watch(val id: String) : DiscoveryEvent
    data class Section(val id: String?) : DiscoveryEvent
    data class Viewed(val id: String) : DiscoveryEvent
}
class DiscoveryViewModel(private val feed: FeedUseCase) : ViewModel() {
    private val mutable=MutableStateFlow(DiscoveryUiState())
    val state=mutable.asStateFlow()
    private var failedWatch: String?=null
    fun sections(now: Instant): List<FeedSection> = mutable.value.let {
        feed.sections(it.lots,it.category,it.query,it.watchedOnly,it.watched,it.recent,now)
    }
    fun event(event: DiscoveryEvent) {
        when(event) {
            DiscoveryEvent.Refresh -> refresh()
            is DiscoveryEvent.Query -> mutable.value=mutable.value.copy(query=event.value,section=null)
            is DiscoveryEvent.Category -> mutable.value=mutable.value.copy(category=event.value,section=null)
            DiscoveryEvent.WatchedOnly -> mutable.value=mutable.value.copy(watchedOnly=!mutable.value.watchedOnly,section=null)
            is DiscoveryEvent.Watch -> toggleWatch(event.id)
            is DiscoveryEvent.Section -> mutable.value=mutable.value.copy(section=event.id)
            is DiscoveryEvent.Viewed -> mutable.value=mutable.value.copy(recent=(listOf(event.id)+mutable.value.recent).distinct().take(20))
        }
    }
    fun refresh() {
        if(mutable.value.loading) return
        failedWatch=null
        mutable.value=mutable.value.copy(loading=true,error=null)
        viewModelScope.launch {
            try {
                val result=feed.load()
                val current=mutable.value
                val watched=(result.watched-current.pending)+(current.watched intersect current.pending)
                mutable.value=current.copy(lots=result.lots,watched=watched,loading=false,loaded=true)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutable.value=mutable.value.copy(loading=false,error=e.message ?: "Could not load nearby scrap. Retry.") }
        }
    }
    fun retry() { failedWatch?.let { toggleWatch(it) } ?: refresh() }
    private fun toggleWatch(id: String) {
        val current=mutable.value
        if(id in current.pending) return
        val wasWatched=id in current.watched
        val enabled=!wasWatched
        mutable.value=current.copy(watched=if(enabled) current.watched+id else current.watched-id,pending=current.pending+id,error=null)
        viewModelScope.launch {
            try { feed.watch(id,enabled); if(failedWatch==id) failedWatch=null }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                val latest=mutable.value
                failedWatch=id
                mutable.value=latest.copy(watched=if(wasWatched) latest.watched+id else latest.watched-id,
                    error=e.message ?: "Could not update watchlist. Retry.")
            } finally { mutable.value=mutable.value.copy(pending=mutable.value.pending-id) }
        }
    }
}

data class LotDetailState(val lot: NearbyLot?=null,val watched: Boolean=false,val loading: Boolean=false,val watching: Boolean=false,val error: String?=null)
class LotDetailViewModel(private val discovery: DiscoveryUseCase): ViewModel() {
    private val mutable=MutableStateFlow(LotDetailState())
    val state=mutable.asStateFlow()
    fun refresh(id: String) {
        mutable.value=mutable.value.copy(loading=true,error=null)
        viewModelScope.launch {
            try {
                val lot=discovery.detail(id)
                val watched=discovery.watched().any { it.id==id }
                mutable.value=mutable.value.copy(lot=lot,watched=watched,loading=false)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutable.value=mutable.value.copy(loading=false,error=e.message ?: "Could not load lot details.") }
        }
    }
    fun watch(id: String) {
        if(mutable.value.watching) return
        val previous=mutable.value.watched
        mutable.value=mutable.value.copy(watched=!previous,watching=true,error=null)
        viewModelScope.launch {
            try { discovery.watch(id,!previous) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutable.value=mutable.value.copy(watched=previous,error=e.message ?: "Could not update watchlist.") }
            finally { mutable.value=mutable.value.copy(watching=false) }
        }
    }
}
