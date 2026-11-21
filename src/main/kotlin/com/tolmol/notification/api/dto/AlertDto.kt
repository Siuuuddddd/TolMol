package com.tolmol.notification.api.dto

import java.util.UUID
data class AlertDto(val eventId: UUID,val type: String,val aggregate: UUID,val data: Map<String,String>)
