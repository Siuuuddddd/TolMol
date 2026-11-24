package com.tolmol.app.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role as AccessibilityRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tolmol.app.R
import com.tolmol.app.feature.auth.domain.Role

private val pixelFont = FontFamily(Font(R.font.pixelify_sans))
private val panel = CutCornerShape(8.dp)

@Composable
fun WelcomeScreen(signIn: () -> Unit, register: (Role) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (LocalDensity.current.fontScale > 1f || maxHeight < 560.dp || maxWidth / maxHeight > 0.55f) {
            ConveyorScreen("What brings you here?") {
                RolePanel("Sell scrap", "List a lot and choose an offer.") { register(Role.SELLER) }
                RolePanel("Buy scrap nearby", "Find local lots and bid per kg.") { register(Role.BUYER) }
                Text("You can do both with one account.")
                Button(signIn, Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = panel) { Text("Sign in") }
            }
        } else {
            // Measure controls against the artwork so their hit areas scale with it.
            val width = maxWidth
            val height = maxHeight
            Image(
                painter = painterResource(R.drawable.tolmol_welcome),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
            Box(Modifier.offset(width * 0.06f, height * 0.08f).size(width * 0.48f, height * 0.15f)
                .semantics { heading(); contentDescription = "TolMol. What brings you here?" })
            ArtworkButton(
                Modifier.offset(width * 0.02f, height * 0.457f).size(width * 0.96f, height * 0.133f),
                "Sell scrap. List a lot and choose an offer.",
            ) { register(Role.SELLER) }
            ArtworkButton(
                Modifier.offset(width * 0.02f, height * 0.605f).size(width * 0.96f, height * 0.139f),
                "Buy scrap nearby. Find local lots and bid per kg.",
            ) { register(Role.BUYER) }
            Box(Modifier.offset(width * 0.045f, height * 0.76f).size(width * 0.9f, height * 0.035f)
                .semantics { contentDescription = "You can do both with one account." })
            ArtworkButton(
                Modifier.offset(width * 0.037f, height * 0.805f).size(width * 0.926f, height * 0.115f),
                "Sign in",
                signIn,
            )
        }
    }
}

@Composable
private fun ArtworkButton(modifier: Modifier, label: String, onClick: () -> Unit) {
    Box(modifier.semantics { contentDescription = label }.clickable(role = AccessibilityRole.Button, onClick = onClick))
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
        Text("TolMol", fontFamily = pixelFont, fontSize = 48.sp, lineHeight = 52.sp, color = MaterialTheme.colorScheme.primary)
        Text(title, fontFamily = pixelFont, fontSize = 27.sp, lineHeight = 32.sp)
        Image(painterResource(R.drawable.scrap_conveyor), contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(2.25f), contentScale = ContentScale.Crop)
        content()
    }
}

@Composable
private fun RolePanel(title: String, subtitle: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = panel,
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
