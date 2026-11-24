package com.tolmol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tolmol.app.core.network.DebugNetworkPermission
import com.tolmol.app.core.ui.TolMolTheme
import com.tolmol.app.navigation.TolMolNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TolMolTheme { DebugNetworkPermission { TolMolNavigation() } } }
    }
}
