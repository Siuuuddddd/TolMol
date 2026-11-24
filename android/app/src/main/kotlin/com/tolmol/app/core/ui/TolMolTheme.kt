package com.tolmol.app.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = darkColorScheme(
    primary = Color(0xFFCBFA33),
    onPrimary = Color(0xFF151900),
    background = Color(0xFF121319),
    surface = Color(0xFF202129),
)

@Composable
fun TolMolTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors) { Surface(content = content) }
}
