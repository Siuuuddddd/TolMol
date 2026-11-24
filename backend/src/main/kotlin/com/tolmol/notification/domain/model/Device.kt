package com.tolmol.notification.domain.model
import java.util.UUID
enum class Platform { ANDROID, IOS }
data class Device(val id: UUID,val platform: Platform,val token: String)
