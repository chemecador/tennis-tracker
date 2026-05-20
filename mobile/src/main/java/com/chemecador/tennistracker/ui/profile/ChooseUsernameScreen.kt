package com.chemecador.tennistracker.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChooseUsernameScreen(
    uid: String,
    onSignOut: () -> Unit,
    viewModel: ChooseUsernameViewModel = koinViewModel(),
) {
    val username by viewModel.username.collectAsStateWithLifecycle()
    val validation by viewModel.validation.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Elige tu username",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Así te encontrarán tus amigos y se identificarán tus partidos.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = username,
            onValueChange = viewModel::onUsernameChange,
            label = { Text("Username") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.AlternateEmail, contentDescription = null)
            },
            trailingIcon = { ValidationTrailingIcon(validation) },
            supportingText = { ValidationSupportingText(validation) },
            isError = validation.isError(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
            ),
            enabled = !isSubmitting,
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
            onClick = { viewModel.submit(uid) },
            enabled = !isSubmitting && validation is UsernameValidation.Available,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Crear cuenta")
            }
        }

        TextButton(
            onClick = onSignOut,
            enabled = !isSubmitting,
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun ValidationTrailingIcon(validation: UsernameValidation) {
    when (validation) {
        UsernameValidation.Available -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
        )

        UsernameValidation.Checking -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )

        UsernameValidation.BadFormat,
        UsernameValidation.Reserved,
        UsernameValidation.Taken -> Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )

        UsernameValidation.Empty -> Unit
    }
}

@Composable
private fun ValidationSupportingText(validation: UsernameValidation) {
    val text = when (validation) {
        UsernameValidation.Empty -> "3-20 caracteres: minúsculas, números, . o _"
        UsernameValidation.BadFormat -> "Formato no válido (3-20: a-z, 0-9, . _)"
        UsernameValidation.Reserved -> "Ese username está reservado"
        UsernameValidation.Checking -> "Comprobando disponibilidad..."
        UsernameValidation.Available -> "Disponible"
        UsernameValidation.Taken -> "Ya está en uso"
    }
    Text(text = text)
}

private fun UsernameValidation.isError(): Boolean = when (this) {
    UsernameValidation.BadFormat,
    UsernameValidation.Reserved,
    UsernameValidation.Taken -> true

    else -> false
}
