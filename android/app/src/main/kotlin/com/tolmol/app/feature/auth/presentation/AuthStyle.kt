package com.tolmol.app.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tolmol.app.MainActivity
import com.tolmol.app.R
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


@Composable
fun rememberGoogleSignIn(viewModel: AuthViewModel): () -> Unit {
    val activity = LocalContext.current.findActivity() as? MainActivity
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }

    return launch@ {
        if (pending || viewModel.state.value.loading) return@launch
        val controller = activity?.googleSignIn
        if (controller == null) { viewModel.showError("Sign-in is unavailable. Reopen TolMol."); return@launch }
        pending = true
        viewModel.clearError()
        scope.launch {
            try {
                val token = controller.google()
                viewModel.google(token)
            } catch (_: CancellationException) {
                // Dismissing the account chooser returns to the current screen.
            } catch (error: Exception) {
                viewModel.showError(error.message ?: "Sign-in failed. Retry.")
            } finally { pending = false }
        }
    }
}

@Composable
fun SocialButtons(viewModel: AuthViewModel, enabled: Boolean, label: String = "or continue with") {
    val launch = rememberGoogleSignIn(viewModel)
    val active = enabled
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(Modifier.weight(1f))
            Text(label, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            OutlinedIconButton({ launch() }, enabled = active,
                modifier = Modifier.size(52.dp), shape = CircleShape,
                colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = Color.White),
                border = BorderStroke(1.dp,Color.LightGray)) {
                Image(painterResource(R.drawable.google_sign_in),"Continue with Google",Modifier.size(24.dp))
            }
        }

    }
}
