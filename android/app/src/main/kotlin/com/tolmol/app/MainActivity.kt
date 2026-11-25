package com.tolmol.app

import android.os.Bundle
import com.tolmol.app.feature.auth.presentation.GoogleSignInController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tolmol.app.core.network.DebugNetworkPermission
import com.tolmol.app.core.ui.TolMolTheme
import com.tolmol.app.navigation.TolMolNavigation

class MainActivity : ComponentActivity() {
    val googleSignIn by lazy { GoogleSignInController(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { TolMolTheme { DebugNetworkPermission { TolMolNavigation() } } }
    }
}
