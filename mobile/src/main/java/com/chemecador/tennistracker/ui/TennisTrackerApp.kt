package com.chemecador.tennistracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chemecador.tennistracker.ui.auth.AuthViewModel
import com.chemecador.tennistracker.ui.auth.LoginScreen
import com.chemecador.tennistracker.ui.home.HomeScreen

@Composable
fun TennisTrackerApp() {
    MaterialTheme {
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
