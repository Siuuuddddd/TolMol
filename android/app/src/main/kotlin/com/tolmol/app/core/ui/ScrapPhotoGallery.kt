package com.tolmol.app.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.tolmol.app.R
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
fun ScrapPhotoGallery(imageKey: String?, category: String, modifier: Modifier = Modifier,
                      fullBleed: Boolean = false, watched: Boolean = false,
                      watching: Boolean = false, onWatch: (() -> Unit)? = null) {
    val pair = productPhotoPairs.firstOrNull { imageKey in it }
    // Show the feed's selected photo first, including alternate demo thumbnails.
    val photos = if (pair == null) listOf(imageKey) else listOf(imageKey) + pair.filter { it != imageKey }
    androidx.compose.runtime.key(imageKey) {
        val pager = rememberPagerState(pageCount = { photos.size })
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
            HorizontalPager(pager, Modifier.fillMaxWidth().aspectRatio(if(fullBleed) 1.12f else 1.3f)
                .then(if(fullBleed) Modifier else Modifier.clip(MaterialTheme.shapes.large))) { page ->
                ScrapImage(photos[page], category, Modifier.fillMaxSize())
            }
            if(fullBleed) {
                Surface(Modifier.align(Alignment.BottomStart).padding(16.dp),shape=RoundedCornerShape(20.dp),
                    color=MaterialTheme.colorScheme.surface.copy(alpha=0.9f)) {
                    Text("${pager.currentPage+1} / ${photos.size}",Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall)
                }
                if(onWatch!=null) Surface(Modifier.align(Alignment.BottomEnd).padding(16.dp),shape=RoundedCornerShape(24.dp),
                    color=MaterialTheme.colorScheme.surface.copy(alpha=0.9f)) {
                    IconButton(onWatch,enabled=!watching) {
                        NativeIcon(if(watched) R.drawable.ic_ui_heart_filled else R.drawable.ic_ui_heart,
                            if(watched) "Remove from watchlist" else "Add to watchlist")
                    }
                }
            }
            }
            if (photos.size > 1 && !fullBleed) {
                Text("Photo ${pager.currentPage + 1} of ${photos.size} · Swipe for more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
