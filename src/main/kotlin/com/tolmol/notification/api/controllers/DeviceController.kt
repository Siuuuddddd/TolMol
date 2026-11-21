package com.tolmol.notification.api.controllers
import com.tolmol.notification.service.DeviceService
import com.tolmol.notification.api.dto.DeviceRequest
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.UUID
@RestController
@RequestMapping("/api/me/devices")
class DeviceController(private val devices: DeviceService) {
    @PostMapping fun register(p: Principal,@RequestBody req: DeviceRequest) =
        mapOf("id" to devices.register(UUID.fromString(p.name),req.platform,req.token))
    @DeleteMapping("/{id}") fun remove(p: Principal,@PathVariable id: UUID) = devices.remove(UUID.fromString(p.name),id)
}
