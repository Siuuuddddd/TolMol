package com.tolmol.notification.domain.model

import java.util.UUID
data class Alert(val eventId: UUID,val type: String,val aggregate: UUID,val data: Map<String,String>,val cursor: Long?=null,val read: Boolean=false)
