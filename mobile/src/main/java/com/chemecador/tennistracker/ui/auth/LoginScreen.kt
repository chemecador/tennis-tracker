package com.chemecador.tennistracker.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chemecador.tennistracker.auth.GoogleCredentialClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: AuthViewModel = viewModel()) {
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleClient = remember(context) { GoogleCredentialClient(context) }

    var mode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Tennis Tracker",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Text(
            text = when (mode) {
                AuthMode.LOGIN -> "Inicia sesión con tu cuenta"
                AuthMode.REGISTER -> "Crea una cuenta nueva"
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = {
                when (mode) {
                    AuthMode.LOGIN -> viewModel.signIn(email, password)
                    AuthMode.REGISTER -> viewModel.register(email, password)
                }
            },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isWorking) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(
                    when (mode) {
                        AuthMode.LOGIN -> "Entrar"
                        AuthMode.REGISTER -> "Registrarse"
                    }
                )
            }
        }

        TextButton(
            onClick = {
                mode = if (mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
                viewModel.clearError()
            },
            enabled = !isWorking,
        ) {
            Text(
                when (mode) {
                    AuthMode.LOGIN -> "¿No tienes cuenta? Regístrate"
                    AuthMode.REGISTER -> "¿Ya tienes cuenta? Inicia sesión"
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    runCatching { googleClient.requestIdToken() }
                        .onSuccess { viewModel.signInWithGoogle(it) }
                        .onFailure {
                            if (it !is GetCredentialCancellationException) {
                                viewModel.onAuthError(it.message ?: "Google sign-in failed")
                            }
                        }
                }
            },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continuar con Google")
        }

        OutlinedButton(
            onClick = { viewModel.signInAsGuest() },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continuar como invitado")
        }
    }
}

private enum class AuthMode { LOGIN, REGISTER }
