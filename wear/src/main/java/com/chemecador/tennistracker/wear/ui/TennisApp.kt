package com.chemecador.tennistracker.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.chemecador.tennistracker.core.auth.AuthViewModel
import com.chemecador.tennistracker.core.match.MatchSessionViewModel
import com.chemecador.tennistracker.wear.ui.auth.LoginScreen
import com.chemecador.tennistracker.wear.ui.match.ScoreboardScreen
import com.chemecador.tennistracker.wear.ui.setup.SetupMatchScreen
import com.chemecador.tennistracker.wear.ui.summary.MatchSummaryScreen
import org.koin.androidx.compose.koinViewModel

private object Routes {
    const val LOGIN = "login"
    const val SETUP = "setup"
    const val MATCH = "match"
    const val SUMMARY = "summary"
}

@Composable
fun TennisApp() {
    MaterialTheme {
        AppScaffold {
            val nav = rememberSwipeDismissableNavController()
            val authVm: AuthViewModel = koinViewModel()
            val user by authVm.user.collectAsStateWithLifecycle()
            val sessionVm: MatchSessionViewModel = koinViewModel()
            val state by sessionVm.state.collectAsStateWithLifecycle()

            LaunchedEffect(user) {
                if (user != null && nav.currentDestination?.route == Routes.LOGIN) {
                    nav.navigate(Routes.SETUP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }

            LaunchedEffect(state?.winner) {
                if (state?.winner != null && nav.currentDestination?.route != Routes.SUMMARY) {
                    nav.navigate(Routes.SUMMARY) {
                        popUpTo(Routes.MATCH) { inclusive = true }
                    }
                }
            }

            val startDestination = if (user != null) Routes.SETUP else Routes.LOGIN

            SwipeDismissableNavHost(
                navController = nav,
                startDestination = startDestination,
            ) {
                composable(Routes.LOGIN) {
                    LoginScreen(viewModel = authVm)
                }
                composable(Routes.SETUP) {
                    SetupMatchScreen(
                        onStart = { config ->
                            sessionVm.start(config)
                            nav.navigate(Routes.MATCH)
                        },
                    )
                }
                composable(Routes.MATCH) {
                    ScoreboardScreen(viewModel = sessionVm)
                }
                composable(Routes.SUMMARY) {
                    MatchSummaryScreen(
                        state = state,
                        onNewMatch = {
                            sessionVm.reset()
                            nav.popToSetup()
                        },
                    )
                }
            }
        }
    }
}

private fun NavHostController.popToSetup() {
    navigate(Routes.SETUP) {
        popUpTo(Routes.SETUP) { inclusive = true }
    }
}
