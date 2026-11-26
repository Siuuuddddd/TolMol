package com.tolmol.app.feature.location.presentation

import com.tolmol.app.core.ui.components.*
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tolmol.app.core.ui.ScreenViewModel
import com.tolmol.app.feature.auth.domain.Role
import com.tolmol.app.feature.location.domain.LocationUseCase
import kotlinx.coroutines.delay
import java.time.Instant

class LocationViewModel(private val location: LocationUseCase) : ScreenViewModel<Instant>() {
    fun refresh(role: Role) = load { location.refresh(role) }
}

@Composable
fun LocationStatus(role: Role, viewModel: LocationViewModel, onRefresh: (Instant) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onRefresh)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refresh(role)
    }
    LaunchedEffect(role, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refresh(role)
                delay(5 * 60_000L)
            }
        }
    }
    LaunchedEffect(role, state.value) { state.value?.let { callback(it) } }
    if (state.error != null) {
        BrickCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            BrickSecondaryButton({
                if (state.error.orEmpty().startsWith("Allow location")) {
                    permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                } else viewModel.refresh(role)
            }) { Text("Refresh location") }
        }
    }
}
