package com.tolmol.notification.infra.database.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tolmol.notification.domain.model.Alert
import com.tolmol.notification.infra.database.entities.AlertEntity
fun AlertEntity.toDomain(json: ObjectMapper) = Alert(eventId,kind,aggregateId,json.readValue<Map<String,String>>(data),sequence,isRead)
