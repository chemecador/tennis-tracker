package com.chemecador.tennistracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chemecador.tennistracker.data.profile.UserProfile
import com.chemecador.tennistracker.ui.auth.AuthViewModel
import com.chemecador.tennistracker.ui.auth.LoginScreen
import com.chemecador.tennistracker.ui.friends.FriendsScreen
import com.chemecador.tennistracker.ui.match.MatchSessionViewModel
import com.chemecador.tennistracker.ui.match.ScoreboardScreen
import com.chemecador.tennistracker.ui.profile.ChooseUsernameScreen
import com.chemecador.tennistracker.ui.profile.ProfileScreen
import com.chemecador.tennistracker.ui.profile.UserProfileViewModel
import com.chemecador.tennistracker.ui.setup.SetupMatchScreen
import com.chemecador.tennistracker.ui.summary.MatchSummaryScreen
import com.chemecador.tennistracker.ui.theme.TennisTrackerTheme
import com.google.firebase.auth.FirebaseUser

private enum class Step { SETUP, MATCH, SUMMARY }

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

            when {
                current == null -> LoginScreen(viewModel = authVm)
                current.isAnonymous -> AppShell(
                    uid = current.uid,
                    profile = null,
                    onSignOut = { authVm.signOut() },
                )

                else -> ProfileGate(
                    user = current,
                    onSignOut = { authVm.signOut() },
                )
            }
        }
    }
}

@Composable
private fun ProfileGate(user: FirebaseUser, onSignOut: () -> Unit) {
    val profileVm: UserProfileViewModel = viewModel(key = "profile-${user.uid}") {
        UserProfileViewModel(uid = user.uid)
    }
    val profile by profileVm.profile.collectAsStateWithLifecycle()
    val isLoading by profileVm.isLoading.collectAsStateWithLifecycle()

    when {
        isLoading -> LoadingScreen()
        profile == null -> ChooseUsernameScreen(uid = user.uid, onSignOut = onSignOut)
        else -> AppShell(uid = user.uid, profile = profile, onSignOut = onSignOut)
    }
}

@Composable
private fun AppShell(uid: String, profile: UserProfile?, onSignOut: () -> Unit) {
    var showingProfile by remember { mutableStateOf(false) }
    var showingFriends by remember { mutableStateOf(false) }

    when {
        showingFriends && profile != null -> FriendsScreen(
            myUid = uid,
            onBack = { showingFriends = false },
        )

        showingProfile -> ProfileScreen(
            profile = profile,
            onBack = { showingProfile = false },
            onSignOut = onSignOut,
            onOpenFriends = { showingFriends = true },
        )

        else -> MatchFlow(
            accountLabel = profile?.username ?: "Modo invitado",
            onOpenProfile = { showingProfile = true },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MatchFlow(accountLabel: String, onOpenProfile: () -> Unit) {
    val sessionVm: MatchSessionViewModel = viewModel()
    val state by sessionVm.state.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf(Step.SETUP) }

    val winner = state?.winner
    if (winner != null && step == Step.MATCH) {
        step = Step.SUMMARY
    }

    when (step) {
        Step.SETUP -> SetupMatchScreen(
            accountLabel = accountLabel,
            onStart = { config ->
                sessionVm.start(config)
                step = Step.MATCH
            },
            onOpenProfile = onOpenProfile,
        )

        Step.MATCH -> ScoreboardScreen(
            viewModel = sessionVm,
            onExit = {
                sessionVm.reset()
                step = Step.SETUP
            },
        )

        Step.SUMMARY -> MatchSummaryScreen(
            state = state,
            onNewMatch = {
                sessionVm.reset()
                step = Step.SETUP
            },
        )
    }
}
