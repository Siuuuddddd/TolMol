package com.tolmol.notification.api.mappers

import com.tolmol.notification.domain.model.Alert
import com.tolmol.notification.api.dto.AlertDto
fun Alert.toDto() = AlertDto(eventId,type,aggregate,data)
