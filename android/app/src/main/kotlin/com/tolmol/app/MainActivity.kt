package com.tolmol.app

import android.os.Bundle
import android.content.Intent
import com.tolmol.app.feature.auth.presentation.SocialSignIn
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tolmol.app.core.network.DebugNetworkPermission
import com.tolmol.app.core.ui.TolMolTheme
import com.tolmol.app.navigation.TolMolNavigation

class MainActivity : ComponentActivity() {
    val socialSignIn by lazy { SocialSignIn(this) }
    @Deprecated("Facebook SDK callback bridge")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        socialSignIn.onActivityResult(requestCode,resultCode,data)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { TolMolTheme { DebugNetworkPermission { TolMolNavigation() } } }
    }
}
