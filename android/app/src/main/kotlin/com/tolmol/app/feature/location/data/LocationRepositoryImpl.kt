package com.tolmol.app.feature.location.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tolmol.app.core.network.serverMessage
import com.tolmol.app.feature.auth.domain.Role
import com.tolmol.app.feature.location.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path
import java.time.Instant

@Serializable data class PositionDto(val latitude: Double, val longitude: Double)

interface LocationApi {
    @PUT("api/me/location/{role}") suspend fun update(@Path("role") role: String, @Body body: PositionDto)
}

class LocationRepositoryImpl(private val context: Context, private val api: LocationApi) : LocationRepository {
    override suspend fun refresh(role: Role): Instant {
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) throw LocationError.PermissionDenied
        val cancellation = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(30_000)
            .setDurationMillis(15_000)
            .build()
        return try {
            val position = LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(request, cancellation.token).await() ?: throw LocationError.Unavailable
            api.update(role.name, PositionDto(position.latitude, position.longitude))
            Instant.now()
        } catch (error: CancellationException) {
            throw error
        } catch (error: SecurityException) {
            throw LocationError.PermissionDenied
        } catch (error: LocationError) {
            throw error
        } catch (error: Exception) {
            throw LocationError.UpdateFailed(error.serverMessage())
        } finally {
            cancellation.cancel()
        }
    }
}
