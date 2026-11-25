package com.tolmol.app.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tolmol.app.R
import com.tolmol.app.feature.auth.domain.Role

private val panel = CutCornerShape(6.dp)

@Composable
fun WelcomeScreen(state: AuthUiState, viewModel: AuthViewModel, signIn: () -> Unit, register: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF121319))
        .verticalScroll(rememberScrollState()).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(340.dp)) {
            Image(painterResource(R.drawable.scrap_conveyor),null,Modifier.matchParentSize(),contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(Color(0xDD121319), Color.Transparent), endX = 800f)))
            Column(Modifier.statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TolMol",fontFamily = authPixelFont,fontWeight = FontWeight.Bold,fontSize = 42.sp,lineHeight = 46.sp,color = MaterialTheme.colorScheme.primary)
                Text("What brings\nyou here?",fontFamily = authPixelFont,fontWeight = FontWeight.Bold,fontSize = 26.sp,lineHeight = 29.sp,color = Color.White)
            }
        }
        Column(Modifier.fillMaxWidth(0.86f).widthIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoPanel("Sell scrap","List a lot and choose an offer.")
            InfoPanel("Buy scrap nearby","Find local lots and bid per kg.")
        }
        Text("You can do both with one account.",Modifier.padding(top = 12.dp,bottom = 18.dp),style = MaterialTheme.typography.bodySmall)
        Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(signIn,Modifier.fillMaxWidth().heightIn(min = 52.dp),enabled = !state.loading,shape = panel) {
                Text("Sign in",fontFamily = authPixelFont,fontWeight = FontWeight.Bold,fontSize = 21.sp)
            }
            OutlinedButton(register,Modifier.fillMaxWidth().heightIn(min = 52.dp),enabled = !state.loading,shape = panel) {
                Text("Create an account",fontFamily = authPixelFont,fontWeight = FontWeight.Bold,fontSize = 20.sp)
            }
            state.error?.let { Text(it,color = MaterialTheme.colorScheme.error,style = MaterialTheme.typography.bodySmall) }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            SocialButtons(viewModel,!state.loading)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InfoPanel(title: String, subtitle: String) {
    OutlinedCard(Modifier.fillMaxWidth(),shape = panel,border = BorderStroke(1.dp,Color(0xFF7D808C)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF30323F))) {
        Column(Modifier.padding(horizontal = 16.dp,vertical = 10.dp),verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title,fontFamily = authPixelFont,fontWeight = FontWeight.Bold,fontSize = 19.sp,lineHeight = 22.sp,color = Color.White)
            Text(subtitle,style = MaterialTheme.typography.bodySmall,color = Color(0xFFE1E1E8))
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
        Text("TolMol", fontFamily = authPixelFont, fontSize = 48.sp, lineHeight = 52.sp, color = MaterialTheme.colorScheme.primary)
        Text(title, fontFamily = authPixelFont, fontSize = 27.sp, lineHeight = 32.sp)
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
