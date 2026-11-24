package com.tolmol.platform.domain.model

import java.util.UUID
data class LiveEvent(val id: UUID,val type: String,val aggregate: UUID,val audience: Set<UUID>,val data: Map<String,String>)
