package com.tolmol.app.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tolmol.app.feature.auth.domain.Role

@Composable
fun AuthForm(state: AuthUiState, register: Boolean, viewModel: AuthViewModel, toggleMode: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var buyer by rememberSaveable { mutableStateOf(true) }
    var seller by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("TolMol", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(if (register) "Create an account" else "Sign in", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(), singleLine = true)
        if (register) {
            Text("Choose at least one role")
            Row {
                FilterChip(buyer, { buyer = !buyer }, label = { Text("Buyer") })
                Spacer(Modifier.width(8.dp))
                FilterChip(seller, { seller = !seller }, label = { Text("Seller") })
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = {
            if (register) {
                val roles = buildSet {
                    if (buyer) add(Role.BUYER)
                    if (seller) add(Role.SELLER)
                }
                viewModel.register(email, password, roles)
            } else viewModel.login(email, password)
        }, enabled = !state.loading && email.isNotBlank() && password.isNotEmpty() && (!register || buyer || seller),
            modifier = Modifier.fillMaxWidth()) {
            Text(if (register) "Create account" else "Sign in")
        }
        TextButton(onClick = toggleMode, enabled = !state.loading) {
            Text(if (register) "Already have an account? Sign in" else "Create an account")
        }
        if (state.error != null) TextButton(onClick = { viewModel.restore() }) { Text("Retry saved session") }
    }
}
