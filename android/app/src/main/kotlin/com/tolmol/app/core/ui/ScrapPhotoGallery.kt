package com.tolmol.app.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val productPhotoPairs = listOf(
    listOf("scrap_plastic_01", "scrap_plastic_02"),
    listOf("scrap_plastic_03", "scrap_plastic_04"),
    listOf("scrap_electronics_06", "scrap_electronics_07"),
    listOf("scrap_electronics_08", "scrap_electronics_09"),
    listOf("scrap_electronics_10", "scrap_electronics_11"),
    listOf("scrap_furniture_01", "scrap_furniture_02"),
    listOf("scrap_furniture_03", "scrap_furniture_04"),
    listOf("scrap_furniture_05", "scrap_furniture_06"),
    listOf("scrap_furniture_07", "scrap_furniture_08"),
)

@Composable
fun ScrapPhotoGallery(imageKey: String?, category: String, modifier: Modifier = Modifier) {
    val pair = productPhotoPairs.firstOrNull { imageKey in it }
    // Show the feed's selected photo first, including alternate demo thumbnails.
    val photos = if (pair == null) listOf(imageKey) else listOf(imageKey) + pair.filter { it != imageKey }
    androidx.compose.runtime.key(imageKey) {
        val pager = rememberPagerState(pageCount = { photos.size })
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalPager(pager, Modifier.fillMaxWidth().aspectRatio(1.3f).clip(MaterialTheme.shapes.large)) { page ->
                ScrapImage(photos[page], category, Modifier.fillMaxSize())
            }
            if (photos.size > 1) {
                Text("Photo ${pager.currentPage + 1} of ${photos.size} · Swipe for more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
