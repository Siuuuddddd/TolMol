package com.tolmol.notification.service
import com.tolmol.notification.domain.model.*
import org.springframework.stereotype.Service
import java.util.UUID

interface DeviceStore {
    fun register(user: UUID,platform: Platform,token: String): UUID
    fun remove(user: UUID,id: UUID)
    fun devices(user: UUID): List<Device>
}
@Service
class DeviceService(private val store: DeviceStore) {
    fun register(user: UUID,platform: Platform,token: String): UUID {
        require(token.isNotBlank() && token.length<=4096 && token.none(Char::isWhitespace))
        if (platform==Platform.IOS) require(token.matches(Regex("[a-fA-F0-9]+")))
        return store.register(user,platform,if (platform==Platform.IOS) token.lowercase() else token)
    }
    fun remove(user: UUID,id: UUID) = store.remove(user,id)
}
