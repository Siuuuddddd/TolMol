package com.tolmol.platform.service

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
interface SocketCommand {
    val type: String
    fun handle(user: UUID,body: JsonNode): Any
}
