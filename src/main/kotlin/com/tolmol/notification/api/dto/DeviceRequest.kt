package com.tolmol.notification.api.dto
import com.tolmol.notification.domain.model.Platform
data class DeviceRequest(val platform: Platform,val token: String)
