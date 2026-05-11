package com.chemecador.tennistracker.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.chemecador.tennistracker.wear.ui.match.MatchSessionViewModel
import com.chemecador.tennistracker.wear.ui.match.ScoreboardScreen
import com.chemecador.tennistracker.wear.ui.setup.SetupMatchScreen
import com.chemecador.tennistracker.wear.ui.summary.MatchSummaryScreen

private object Routes {
    const val SETUP = "setup"
    const val MATCH = "match"
    const val SUMMARY = "summary"
}

@Composable
fun TennisApp() {
    MaterialTheme {
        AppScaffold {
            val nav = rememberSwipeDismissableNavController()
            val sessionVm: MatchSessionViewModel = viewModel()
            val state by sessionVm.state.collectAsStateWithLifecycle()

            LaunchedEffect(state?.winner) {
                if (state?.winner != null && nav.currentDestination?.route != Routes.SUMMARY) {
                    nav.navigate(Routes.SUMMARY) {
                        popUpTo(Routes.MATCH) { inclusive = true }
                    }
                }
            }

            SwipeDismissableNavHost(
                navController = nav,
                startDestination = Routes.SETUP,
            ) {
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
