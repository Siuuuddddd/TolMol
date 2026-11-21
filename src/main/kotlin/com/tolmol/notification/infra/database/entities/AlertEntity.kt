package com.tolmol.notification.infra.database.entities

import java.util.UUID
data class AlertEntity(val eventId: UUID,val kind: String,val aggregateId: UUID,val data: String)
