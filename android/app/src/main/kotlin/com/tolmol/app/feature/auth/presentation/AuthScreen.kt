package com.tolmol.app.feature.auth.presentation

import com.tolmol.app.core.ui.PixelButton as Button

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tolmol.app.R

@Composable
fun AuthForm(state: AuthUiState, register: Boolean, viewModel: AuthViewModel, toggleMode: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val validEmail = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val validPassword = password.length >= 10 && password.toByteArray().size <= 72
    val canSubmit = validEmail && password.isNotEmpty() &&
        (!register || name.trim().length in 1..100 && validPassword)
    fun submit() {
        if (!canSubmit || state.loading) return
        if (register) viewModel.registerProfile(name,email,password) else viewModel.login(email,password)
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).imePadding()) {
                Box(Modifier.fillMaxWidth().height(210.dp)) {
                    Image(painterResource(R.drawable.scrap_conveyor),null,Modifier.matchParentSize(),contentScale = ContentScale.Crop)
                    Text("TolMol",Modifier.padding(24.dp),fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,fontSize = 38.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp,vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (register) "Create an account" else "Sign in",fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,fontSize = 30.sp,lineHeight = 34.sp)
                    if (register) {
                        Text("Fill in your information or continue with a social account.",style = MaterialTheme.typography.bodyMedium)
                        AuthField(name,{name=it},"Name",KeyboardType.Text)
                    }
                    AuthField(email,{email=it},"Email",KeyboardType.Email)
                    OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),
                        label = {Text("Password")},singleLine = true,shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {TextButton({visible=!visible}) {Text(if (visible) "Hide" else "Show")}})
                    if (register) {
                        Text("Use 10 or more characters.",style = MaterialTheme.typography.bodySmall)
                    }
                    state.error?.let {
                        Text(it,color = MaterialTheme.colorScheme.error,style = MaterialTheme.typography.bodySmall)
                        TextButton({submit()},enabled = canSubmit && !state.loading) {Text("Retry")}
                    }
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Button({submit()},Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        enabled = canSubmit && !state.loading) {
                        Text(if (register) "Create account" else "Sign in",fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,fontSize = 22.sp)
                    }
                    SocialButtons(viewModel,!state.loading,if (register) "or sign up with" else "or continue with")
                    TextButton(toggleMode,enabled = !state.loading) {
                        Text(if (register) "Already have an account? Sign in" else "Create an account")
                    }
                }
            }
        }
}

@Composable
private fun AuthField(value: String, change: (String)->Unit, title: String, type: KeyboardType) {
    OutlinedTextField(value,change,Modifier.fillMaxWidth(),label = {Text(title)},
        singleLine = true,shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(keyboardType = type))
}
