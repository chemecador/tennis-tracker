package com.chemecador.tennistracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chemecador.tennistracker.ui.auth.AuthViewModel
import com.chemecador.tennistracker.ui.auth.LoginScreen
import com.chemecador.tennistracker.ui.home.HomeScreen
import com.chemecador.tennistracker.ui.theme.TennisTrackerTheme

@Composable
fun TennisTrackerApp() {
    TennisTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val authVm: AuthViewModel = viewModel()
            val user by authVm.user.collectAsStateWithLifecycle()

            val current = user
            if (current == null) {
                LoginScreen(viewModel = authVm)
            } else {
                HomeScreen(
                    user = current,
                    onSignOut = { authVm.signOut() },
                )
            }
        }
    }
}
