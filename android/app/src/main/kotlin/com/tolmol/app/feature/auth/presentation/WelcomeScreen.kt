package com.tolmol.app.feature.auth.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tolmol.app.core.ui.components.*
import com.tolmol.app.core.ui.theme.*

@Composable
fun WelcomeScreen(state:AuthUiState,viewModel:AuthViewModel,signIn:()->Unit,register:()->Unit) {
    WelcomeSceneContent(state,signIn,register) {SocialButtons(viewModel,!state.loading)}
}
@Composable
fun WelcomeSceneContent(state:AuthUiState,signIn:()->Unit,register:()->Unit,social:@Composable ()->Unit) {
    CreamScene(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewport=maxHeight
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min=viewport),
                verticalArrangement=Arrangement.SpaceBetween,horizontalAlignment=Alignment.CenterHorizontally) {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=GameDimens.AuthMargin,vertical=GameDimens.Gap),
                    verticalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
                    BrickCard(Modifier.fillMaxWidth(),tone=BrickTone.Red) {
                        Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {OutlinedPixelText("TolMol",style=MaterialTheme.typography.displayMedium)}
                    }
                    Box(Modifier.fillMaxWidth().height(136.dp)) {
                        OutlinedPixelText("What brings\nyou here?",Modifier.fillMaxWidth(0.72f).align(Alignment.CenterStart),style=MaterialTheme.typography.headlineLarge)
                        CardboardStack(Modifier.size(116.dp).align(Alignment.CenterEnd))
                    }
                    BrickCard(Modifier.fillMaxWidth(),tone=BrickTone.Blue) {
                        Text("Sell scrap",style=MaterialTheme.typography.titleLarge)
                        Text("List a lot and choose an offer.",style=MaterialTheme.typography.bodyMedium)
                    }
                    BrickCard(Modifier.fillMaxWidth(),tone=BrickTone.Yellow) {
                        Text("Buy scrap nearby",style=MaterialTheme.typography.titleLarge)
                        Text("Find local lots and bid per kg.",style=MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal=GameDimens.AuthMargin,vertical=GameDimens.Gap),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(GameDimens.SmallGap)) {
                    BrickButton(signIn,Modifier.fillMaxWidth(),enabled=!state.loading) {Text("Sign in",style=MaterialTheme.typography.titleLarge)}
                    YellowUnderlineLink("Create an account",register,enabled=!state.loading)
                    state.error?.let {Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
                    if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    social()
                }
                Column(Modifier.fillMaxWidth().background(GameColors.Stone).navigationBarsPadding()) {BrickWallFooter()}
            }
        }
    }
}
@Preview(name="Role selection / onboarding",widthDp=390,heightDp=844)
@Composable
fun RoleSelectionPreview() {TolMolTheme {WelcomeSceneContent(AuthUiState(loading=false),{}, {}) {
    SocialSignInButton({},Modifier.fillMaxWidth())
}}}
