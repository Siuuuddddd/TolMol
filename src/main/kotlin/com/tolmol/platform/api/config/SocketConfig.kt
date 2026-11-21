package com.tolmol.platform.api.config

import com.tolmol.platform.infra.websocket.SocketGateway
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.*
@Configuration
@EnableWebSocket
class SocketConfig(private val gateway: SocketGateway) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(gateway,"/ws")
    }
}
