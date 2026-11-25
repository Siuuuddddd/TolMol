package com.tolmol.app.feature.discovery
import com.tolmol.app.feature.discovery.domain.*
import com.tolmol.app.feature.discovery.presentation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest {
    @Test fun optimisticWatchRollsBackAndRetryRecoversWhileRefreshPreservesCachedContent()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo=FeedRepositoryFake().apply {lots=listOf(feedLot("one","METAL",Instant.now().plusSeconds(3600)))}
            val vm=DiscoveryViewModel(FeedUseCase(DiscoveryUseCase(repo)))
            vm.refresh()
            assertTrue(vm.state.value.loading)
            runCurrent()
            assertTrue(vm.state.value.loaded)
            repo.watchGate=CompletableDeferred()
            vm.event(DiscoveryEvent.Watch("one"))
            assertTrue("one" in vm.state.value.watched)
            runCurrent()
            repo.watchGate!!.completeExceptionally(IllegalStateException("Watch failed"))
            runCurrent()
            assertFalse("one" in vm.state.value.watched)
            assertNotNull(vm.state.value.error)
            repo.watchGate=null
            vm.retry()
            runCurrent()
            assertTrue("one" in vm.state.value.watched)
            assertNull(vm.state.value.error)
            repo.loadFails=true
            vm.refresh()
            runCurrent()
            assertEquals(1,vm.state.value.lots.size)
            assertNotNull(vm.state.value.error)
            repo.loadFails=false
            repo.lots=emptyList()
            vm.retry()
            runCurrent()
            assertTrue(vm.state.value.loaded)
            assertTrue(vm.state.value.lots.isEmpty())
            assertFalse(vm.state.value.loading)
        } finally { Dispatchers.resetMain() }
    }
}
