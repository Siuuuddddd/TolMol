package com.tolmol.app.core.ui.theme
import android.app.Activity
import androidx.compose.foundation.text.selection.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val palette=lightColorScheme(
    primary=GameColors.Red,onPrimary=GameColors.Paper,primaryContainer=GameColors.ErrorLight,onPrimaryContainer=GameColors.Navy,
    secondary=GameColors.Blue,onSecondary=GameColors.Paper,secondaryContainer=GameColors.BlueLight,onSecondaryContainer=GameColors.Navy,
    tertiary=GameColors.Yellow,onTertiary=GameColors.Navy,tertiaryContainer=GameColors.YellowLight,onTertiaryContainer=GameColors.Navy,
    background=GameColors.Cream,onBackground=GameColors.Navy,surface=GameColors.Paper,onSurface=GameColors.Navy,
    surfaceVariant=GameColors.BlueLight,onSurfaceVariant=GameColors.Muted,
    surfaceContainerLowest=GameColors.Paper,surfaceContainerLow=GameColors.Cream,surfaceContainer=GameColors.Cream,
    surfaceContainerHigh=GameColors.YellowLight,surfaceContainerHighest=GameColors.BlueLight,
    surfaceTint=GameColors.Blue,outline=GameColors.Navy,outlineVariant=GameColors.BorderLight,
    error=GameColors.RedShadow,onError=GameColors.Paper,errorContainer=GameColors.ErrorLight,onErrorContainer=GameColors.RedShadow,
    inverseSurface=GameColors.Navy,inverseOnSurface=GameColors.Paper,inversePrimary=GameColors.Yellow)

@Composable
fun TolMolTheme(content:@Composable ()->Unit) {
    val view=LocalView.current
    SideEffect { if(!view.isInEditMode) (view.context as? Activity)?.window?.let {
        WindowCompat.getInsetsController(it,view).isAppearanceLightStatusBars=true
        WindowCompat.getInsetsController(it,view).isAppearanceLightNavigationBars=true
    } }
    CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(GameColors.Blue,GameColors.BlueLight)) {
        MaterialTheme(colorScheme=palette,typography=GameTypography,shapes=GameShapes) {
            Surface(color=GameColors.Cream,contentColor=GameColors.Navy,content=content)
        }
    }
}
