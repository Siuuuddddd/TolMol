package com.tolmol.app.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable
data object Welcome : NavKey

@Composable
fun TolMolNavigation() {
    val stack = rememberNavBackStack(Welcome)
    NavDisplay(backStack = stack, modifier = Modifier.safeDrawingPadding(),
        entryProvider = entryProvider { entry<Welcome> { Text("TolMol") } })
}
