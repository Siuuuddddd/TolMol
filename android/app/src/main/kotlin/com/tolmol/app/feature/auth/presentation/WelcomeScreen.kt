package com.tolmol.app.feature.auth.presentation

import com.tolmol.app.core.ui.PixelOutlinedButton as OutlinedButton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role as AccessibilityRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tolmol.app.R
import com.tolmol.app.feature.auth.domain.Role
import kotlin.math.roundToInt


/** Reuse the approved 853 x 1844 artwork as UI regions, without its old account note. */
@Composable
fun WelcomeScreen(state: AuthUiState, viewModel: AuthViewModel, signIn: () -> Unit, register: () -> Unit) {
    val art = ImageBitmap.imageResource(R.drawable.welcome_factory_blank)
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val viewport = maxHeight
        val heroHeight = (viewport * 0.42f).coerceAtLeast(300.dp)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = viewport).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(heroHeight)) {
                    ArtworkRegion(art, 0, 0, 853, 842, Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth(0.7f).statusBarsPadding().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("TolMol", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                        Text("What brings\nyou here?", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Column(Modifier.fillMaxWidth(0.88f).widthIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FactoryInfoCard(art, 842, 252, "Sell scrap", "List a lot and choose an offer.")
                    FactoryInfoCard(art, 1114, 268, "Buy scrap nearby", "Find local lots and bid per kg.")
                }
            }
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().height(68.dp)
                        .graphicsLayer { alpha = if (state.loading) 0.5f else 1f }
                        .clickable(enabled = !state.loading, role = AccessibilityRole.Button, onClick = signIn),
                        contentAlignment = Alignment.Center) {
                        // Reassemble blank button bands so the lettering is never stretched.
                        Column(Modifier.matchParentSize()) {
                            ArtworkRegion(art, 20, 1482, 813, 70, Modifier.fillMaxWidth().height(20.dp))
                            ArtworkRegion(art, 20, 1552, 813, 6, Modifier.fillMaxWidth().weight(1f))
                            ArtworkRegion(art, 20, 1660, 813, 51, Modifier.fillMaxWidth().height(10.dp))
                        }
                        Text("Sign in", Modifier.padding(top = 7.dp), color = Color(0xFF121319),
                            fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                OutlinedButton(register, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.loading) {
                    Text("Create an account", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                SocialButtons(viewModel, !state.loading)
            }
            ArtworkRegion(art, 0, 1712, 853, 132, Modifier.fillMaxWidth().height(36.dp))
        }
    }
}

@Composable
private fun ArtworkRegion(art: ImageBitmap, x: Int, y: Int, width: Int, height: Int, modifier: Modifier) {
    Canvas(modifier) {
        drawImage(art, srcOffset = IntOffset(x,y), srcSize = IntSize(width,height),
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.None)
    }
}

@Composable
private fun FactoryInfoCard(art: ImageBitmap, y: Int, height: Int, title: String, subtitle: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth
        Box(Modifier.fillMaxWidth().heightIn(min = width * height / 837f)) {
            ArtworkRegion(art, 8, y, 837, height, Modifier.matchParentSize())
            Column(Modifier.padding(start = width * 0.25f, end = width * 0.18f, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
@Composable
fun RolePicker(state: AuthUiState, onSelect: (Role) -> Unit, onLogout: () -> Unit) {
    ConveyorScreen("What brings you here?") {
        val roles = state.account?.roles.orEmpty()
        if (Role.SELLER in roles) {
            RolePanel("Sell scrap", "List a lot and choose an offer.") { onSelect(Role.SELLER) }
        }
        if (Role.BUYER in roles) {
            RolePanel("Buy scrap nearby", "Find local lots and bid per kg.") { onSelect(Role.BUYER) }
        }
        Text("Your roles: ${roles.joinToString { it.name.lowercase() }}", style = MaterialTheme.typography.bodyMedium)
        TextButton(onLogout) { Text("Sign out") }
    }
}

@Composable
private fun ConveyorScreen(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("TolMol", fontSize = 48.sp, lineHeight = 52.sp, color = MaterialTheme.colorScheme.primary)
        Text(title, fontSize = 27.sp, lineHeight = 32.sp)
        Image(painterResource(R.drawable.scrap_conveyor), contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(2.25f), contentScale = ContentScale.Crop)
        content()
    }
}

@Composable
private fun RolePanel(title: String, subtitle: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(10.dp, 30.dp), shape = CutCornerShape(2.dp), color = MaterialTheme.colorScheme.primary) {}
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp)
        }
    }
}
