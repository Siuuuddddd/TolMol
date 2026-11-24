package com.tolmol.app.feature.location.domain

import com.tolmol.app.feature.auth.domain.Role
import java.time.Instant

sealed class LocationError(message: String) : RuntimeException(message) {
    data object PermissionDenied : LocationError("Allow location to browse, bid or publish.")
    data object Unavailable : LocationError("Location is unavailable. Turn on location and retry.")
    class UpdateFailed(message: String) : LocationError(message)
}

interface LocationRepository {
    suspend fun refresh(role: Role): Instant
}

class LocationUseCase(repository: LocationRepository) : LocationRepository by repository
