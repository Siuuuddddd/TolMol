package com.tolmol.app.feature.bidding

import com.tolmol.app.core.common.LiveUpdate
import com.tolmol.app.feature.bidding.domain.*
import com.tolmol.app.feature.bidding.presentation.BiddingViewModel
import com.tolmol.app.feature.bidding.presentation.bidError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BiddingViewModelTest {
    @Test fun retryUsesSameRequestKeyAndShowsCooldown() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val attempts = mutableListOf<String>()
            val snapshot = Auction("lot", BigDecimal.TEN, null, "OPEN", 1)
            val source = object : BiddingRepository {
                override val live = emptyFlow<LiveUpdate>()
                override suspend fun auction(id: String) = snapshot
                override suspend fun subscribe(id: String) = snapshot
                override suspend fun unsubscribe(id: String) = Unit
                override suspend fun place(id: String, rate: BigDecimal, key: String) {
                    attempts += key
                    if (attempts.size == 1) throw BiddingError.RequestFailed("Next bid allowed at 2026-11-25T12:00:00Z")
                }
            }
            val vm = BiddingViewModel(BiddingUseCase(source))
            vm.place("lot", BigDecimal.TEN)
            runCurrent()
            val now = Instant.parse("2026-11-25T10:30:00Z")
            assertEquals("You can bid with this seller again in 1h 30m.", bidError(vm.state.value.error.orEmpty(), now))
            vm.place("lot", BigDecimal("10.00"))
            runCurrent()
            assertEquals(2, attempts.size)
            assertEquals(attempts.first(), attempts.last())
            assertEquals(snapshot, vm.state.value.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun preservesBidAndLocationValidationMessages() {
        val now = Instant.EPOCH
        for (message in listOf("Bid must meet the starting rate and exceed the current leader", "A recent location is required")) {
            assertEquals(message, bidError(message, now))
        }
    }
}
