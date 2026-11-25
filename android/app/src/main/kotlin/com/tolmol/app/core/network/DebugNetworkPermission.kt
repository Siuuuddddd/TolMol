package com.tolmol.app.core.network

import com.tolmol.app.core.ui.PixelOutlinedButton as OutlinedButton

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tolmol.app.BuildConfig

private const val localPermission = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
fun DebugNetworkPermission(content: @Composable () -> Unit) {
    if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37 || !BuildConfig.REST_URL.startsWith("http://")) {
        content()
        return
    }
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(context.checkSelfPermission(localPermission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (granted) {
        content()
    } else {
        Column(Modifier.padding(24.dp)) {
            Text("Allow local network access to connect to your development backend.")
            OutlinedButton({ launcher.launch(localPermission) }) { Text("Allow local network") }
        }
    }
}
