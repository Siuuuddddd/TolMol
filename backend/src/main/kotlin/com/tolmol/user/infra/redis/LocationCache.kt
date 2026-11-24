package com.tolmol.user.infra.redis

import com.tolmol.user.domain.model.Position
import com.tolmol.user.domain.model.Role
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID
import java.time.Duration

@Component
class LocationCache(private val redis: StringRedisTemplate) {
    fun resolve(user: UUID, role: Role, revision: Long, load: () -> Position?): Position? {
        val key = "tolmol:location:$user:$role:$revision"
        try {
            redis.opsForValue().get(key)?.split(",")?.let { coordinates ->
                return Position(coordinates[0].toDouble(), coordinates[1].toDouble())
            }
        } catch (_: DataAccessException) { return load() }
        val position = load() ?: return null
        try {
            redis.opsForValue().set(key, "${position.latitude},${position.longitude}", Duration.ofMinutes(15))
        } catch (_: DataAccessException) { /* The committed location remains usable. */ }
        return position
    }
}
