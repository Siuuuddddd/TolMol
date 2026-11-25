package com.tolmol.app.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val colors = darkColorScheme(
    primary = Color(0xFFCBFA33),
    onPrimary = Color(0xFF151900),
    background = Color(0xFF121319),
    surface = Color(0xFF202129),
    onBackground = Color(0xFFF1F0F5),
    onSurface = Color(0xFFF1F0F5),
    onSurfaceVariant = Color(0xFFD1D2DB),
    primaryContainer = Color(0xFF35450C),
    onPrimaryContainer = Color(0xFFE1FFA0),
    secondary = Color(0xFFCBD5B4),
    onSecondary = Color(0xFF29311E),
    secondaryContainer = Color(0xFF30362B),
    onSecondaryContainer = Color(0xFFE4EDD5),
    tertiary = Color(0xFFD2EB9A),
    onTertiary = Color(0xFF283500),
    tertiaryContainer = Color(0xFF33451F),
    onTertiaryContainer = Color(0xFFE1FFA8),
    surfaceVariant = Color(0xFF30323F),
    surfaceContainerLowest = Color(0xFF101116),
    surfaceContainerLow = Color(0xFF191A21),
    surfaceContainer = Color(0xFF202129),
    surfaceContainerHigh = Color(0xFF292B35),
    surfaceContainerHighest = Color(0xFF333541),
    surfaceTint = Color(0xFFCBFA33),
    outline = Color(0xFF7D808C),
    outlineVariant = Color(0xFF454752),
    inverseSurface = Color(0xFFE4E2E9),
    inverseOnSurface = Color(0xFF202129),
    inversePrimary = Color(0xFF536900),
)

private val pixelShapes = Shapes(
    extraSmall = CutCornerShape(2.dp), small = CutCornerShape(6.dp),
    medium = CutCornerShape(8.dp), large = CutCornerShape(10.dp), extraLarge = CutCornerShape(12.dp),
)

@Composable
fun TolMolTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = TolMolTypography, shapes = pixelShapes) { Surface(content = content) }
}
