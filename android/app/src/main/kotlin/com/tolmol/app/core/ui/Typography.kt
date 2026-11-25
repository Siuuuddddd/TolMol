package com.tolmol.app.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tolmol.app.R

// Bundled SIL OFL font. All Material components and custom text inherit this family.
private val pixelFamily = FontFamily(Font(R.font.pixelify_sans))
private fun pixel(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = pixelFamily, fontWeight = weight, fontSize = size.sp,
    lineHeight = line.sp, letterSpacing = 0.25.sp,
)
val TolMolTypography = Typography(
    displayLarge = pixel(48,56,FontWeight.Bold),
    displayMedium = pixel(40,48,FontWeight.Bold),
    displaySmall = pixel(36,44,FontWeight.Bold),
    headlineLarge = pixel(32,40,FontWeight.Bold),
    headlineMedium = pixel(28,36,FontWeight.Bold),
    headlineSmall = pixel(24,32,FontWeight.Bold),
    titleLarge = pixel(24,32,FontWeight.Bold),
    titleMedium = pixel(20,28,FontWeight.Bold),
    titleSmall = pixel(18,24,FontWeight.Bold),
    bodyLarge = pixel(18,26),
    bodyMedium = pixel(16,24),
    bodySmall = pixel(14,20),
    labelLarge = pixel(18,24,FontWeight.Bold),
    labelMedium = pixel(14,20,FontWeight.Medium),
    labelSmall = pixel(13,18,FontWeight.Medium),
)
