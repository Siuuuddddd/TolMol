package com.tolmol.app.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.tolmol.app.MainActivity
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
fun SocialButtons(viewModel: AuthViewModel, enabled: Boolean, label: String = "or continue with", onSky: Boolean = false) {
    val launch = rememberGoogleSignIn(viewModel)
    Column(verticalArrangement = Arrangement.spacedBy(com.tolmol.app.core.ui.theme.GameDimens.Gap), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(com.tolmol.app.core.ui.theme.GameDimens.Gap)) {
            val ink=if(onSky) com.tolmol.app.core.ui.theme.GameColors.Paper else com.tolmol.app.core.ui.theme.GameColors.Navy
            HorizontalDivider(Modifier.weight(1f),color=ink.copy(alpha=0.5f))
            Text(label,style=MaterialTheme.typography.bodySmall,color=ink)
            HorizontalDivider(Modifier.weight(1f),color=ink.copy(alpha=0.5f))
        }
        com.tolmol.app.core.ui.components.SocialSignInButton(launch,Modifier.fillMaxWidth(),enabled)
    }
}
