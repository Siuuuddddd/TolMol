package com.tolmol
import com.tolmol.notification.service.*
import com.tolmol.notification.domain.model.*
import com.tolmol.platform.domain.exception.BusinessException
import com.tolmol.user.domain.model.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class DeviceTest : MarketFixture() {
    @Autowired lateinit var devices: DeviceService
    @Autowired lateinit var store: DeviceStore
    @Autowired lateinit var provider: PushProvider
    @Test fun tokenBelongsToOneUser() {
        val owner = user(Role.BUYER)
        val other = user(Role.SELLER)
        val id = devices.register(owner,Platform.ANDROID,"mock-fcm-token")
        assertEquals(id,devices.register(owner,Platform.ANDROID,"mock-fcm-token"))
        assertThrows(BusinessException::class.java) { devices.register(other,Platform.ANDROID,"mock-fcm-token") }
        devices.remove(other,id)
        assertEquals(id,store.devices(owner).single().id)
        provider.send(owner,Alert(UUID.randomUUID(),"TEST",UUID.randomUUID(),emptyMap()))
        devices.remove(owner,id)
        assertTrue(store.devices(owner).isEmpty())
    }
}
