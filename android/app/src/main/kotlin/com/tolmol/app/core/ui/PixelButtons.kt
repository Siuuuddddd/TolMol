package com.tolmol.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PixelButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(2.dp,if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        content = content)
}

@Composable
fun PixelOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                        content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick, modifier.heightIn(min = 52.dp), enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(2.dp,MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface),
        content = content)
}
