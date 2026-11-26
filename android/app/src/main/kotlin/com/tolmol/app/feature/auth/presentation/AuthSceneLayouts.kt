package com.tolmol.app.feature.auth.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tolmol.app.R
import com.tolmol.app.core.ui.NativeIcon
import com.tolmol.app.core.ui.components.*
import com.tolmol.app.core.ui.theme.*

@Composable
fun AuthSceneContent(state:AuthUiState,register:Boolean,name:String,onName:(String)->Unit,
    email:String,onEmail:(String)->Unit,password:String,onPassword:(String)->Unit,
    visible:Boolean,onVisibility:()->Unit,canSubmit:Boolean,submit:()->Unit,toggleMode:()->Unit,
    social:@Composable ()->Unit) {
    SkyScene(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
            Spacer(Modifier.statusBarsPadding())
            SignBoard()
            Column(Modifier.fillMaxWidth().padding(horizontal=GameDimens.AuthMargin,vertical=GameDimens.Gap),
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
                OutlinedPixelText(if(register) "Create an account" else "Sign in",style=MaterialTheme.typography.displaySmall)
                if(register) {
                    Text("Fill in your information or continue with a social account.",style=MaterialTheme.typography.bodySmall,color=GameColors.Paper)
                    TolMolTextField(name,onName,Modifier.fillMaxWidth(),label={Text("Name")},singleLine=true,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text))
                }
                TolMolTextField(email,onEmail,Modifier.fillMaxWidth(),label={Text("Email")},singleLine=true,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email))
                TolMolTextField(password,onPassword,Modifier.fillMaxWidth(),label={Text("Password")},singleLine=true,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
                    visualTransformation=if(visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon={IconButton(onVisibility) {NativeIcon(if(visible) R.drawable.ic_ui_visibility_off else R.drawable.ic_ui_visibility,
                        if(visible) "Hide password" else "Show password")}})
                if(register) Text("Use 10 or more characters.",Modifier.fillMaxWidth(),style=MaterialTheme.typography.bodySmall,color=GameColors.Paper)
                state.error?.let {
                    BrickCard(Modifier.fillMaxWidth()) {
                        Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                        TextButton(submit,enabled=canSubmit && !state.loading) {Text("Retry")}
                    }
                }
                if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(),color=GameColors.Yellow,trackColor=GameColors.SkyTop)
                BrickButton(submit,Modifier.fillMaxWidth(),enabled=canSubmit && !state.loading) {
                    Text(if(register) "Create account" else "Sign in",style=MaterialTheme.typography.titleLarge)
                }
                GirderDivider()
                social()
                YellowUnderlineLink(if(register) "Already have an account? Sign in" else "Create an account",
                    toggleMode,enabled=!state.loading,onSky=true)
            }
            RailCartScene()
            Spacer(Modifier.fillMaxWidth().background(GameColors.Stone).navigationBarsPadding())
        }
    }
}

@Preview(name="Sign in sky scene",widthDp=390,heightDp=844)
@Composable
fun SignInPreview() {TolMolTheme {
    AuthSceneContent(AuthUiState(loading=false),false,"",{},"hello@tolmol.demo",{},"password",{},false,{},true,{}, {}) {
        SocialSignInButton({},Modifier.fillMaxWidth())
    }
}}
@Preview(name="Create account sky scene",widthDp=390,heightDp=1000)
@Composable
fun CreateAccountPreview() {TolMolTheme {
    AuthSceneContent(AuthUiState(loading=false),true,"Aarav Sharma",{},"hello@tolmol.demo",{},"password",{},false,{},true,{}, {}) {
        SocialSignInButton({},Modifier.fillMaxWidth())
    }
}}
