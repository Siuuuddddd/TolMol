package com.tolmol.app.core.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tolmol.app.R


private fun fallback(category: String) = when(category.uppercase()) {
    "METAL" -> R.drawable.scrap_placeholder_metal
    "PAPER" -> R.drawable.scrap_placeholder_paper
    "CARDBOARD" -> R.drawable.scrap_placeholder_cardboard
    "PLASTIC" -> R.drawable.scrap_placeholder_plastic
    "PAINT" -> R.drawable.scrap_placeholder_paint
    "ELECTRONICS" -> R.drawable.scrap_placeholder_electronics
    "FURNITURE" -> R.drawable.scrap_placeholder_furniture
    else -> R.drawable.scrap_placeholder_other
}
@Composable
fun ScrapImage(key: String?,category: String,modifier: Modifier=Modifier) {
    Image(painterResource(importedScrapPhotos[key] ?: fallback(category)),contentDescription=null,
        modifier=modifier,contentScale=ContentScale.Crop)
}
