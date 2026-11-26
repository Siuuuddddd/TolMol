package com.tolmol.app.feature.auth.presentation

import android.util.Patterns
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

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
    AuthSceneContent(state,register,name,{name=it},email,{email=it},password,{password=it},
        visible,{visible=!visible},canSubmit,{submit()},toggleMode) {
        SocialButtons(viewModel,!state.loading,if(register) "or sign up with" else "or continue with",onSky=true)
    }
}
