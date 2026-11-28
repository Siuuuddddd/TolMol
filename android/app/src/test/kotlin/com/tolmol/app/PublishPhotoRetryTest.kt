
package com.tolmol.app
import com.tolmol.app.feature.listing.domain.*
import com.tolmol.app.feature.listing.presentation.PublishViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
@OptIn(ExperimentalCoroutinesApi::class)
class PublishPhotoRetryTest {
    @Test fun failedUploadRetriesSameListingAndSkipsSuccessfulPhotos() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var creates=0
            val attempts=mutableListOf<String>()
            var fail=true
            val repository=object: ListingRepository {
                override suspend fun mine(after: String?)=emptyList<Listing>()
                override suspend fun offers(id: String)=emptyList<Offer>()
                override suspend fun accept(id: String,bidId: String) {}
                override suspend fun publish(category: String,weight: BigDecimal,startingRate: BigDecimal,deadline: String,title: String?,description: String?): Listing {
                    creates++
                    return Listing("created",category,weight,startingRate,deadline,"OPEN")
                }
            }
            val photoRepository=object: ListingPhotoRepository {
                override suspend fun prepare(source: String)=PreparedPhoto(source,source,byteArrayOf(1))
                override suspend fun upload(listing: String,photo: PreparedPhoto) {
                    assertEquals("created",listing);attempts.add(photo.key)
                    if(photo.key=="two" && fail) {fail=false;error("Temporary upload failure")}
                }
            }
            val vm=PublishViewModel(ListingUseCase(repository),UploadListingPhotosUseCase(photoRepository))
            vm.add(listOf("one","two"));advanceUntilIdle()
            vm.publish("PAPER",BigDecimal.TEN,BigDecimal.ONE,24,"Paper","Details");advanceUntilIdle()
            assertNotNull(vm.state.value.error)
            assertNull(vm.state.value.value)
            assertEquals(2,vm.selected.size)
            vm.publish("PAPER",BigDecimal.TEN,BigDecimal.ONE,24,"Paper","Details");advanceUntilIdle()
            assertEquals(1,creates)
            assertEquals(listOf("one","two","two"),attempts)
            assertNotNull(vm.state.value.value)
        } finally {Dispatchers.resetMain()}
    }
}
