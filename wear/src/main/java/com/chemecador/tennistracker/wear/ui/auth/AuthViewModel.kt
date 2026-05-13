package com.chemecador.tennistracker.wear.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.tennistracker.wear.auth.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    val user: StateFlow<FirebaseUser?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.currentUser)

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signInAsGuest() = launchAuth { repository.signInAnonymously() }

    fun signInWithGoogle(idTokenProvider: suspend () -> String) = launchAuth {
        val idToken = idTokenProvider()
        repository.signInWithGoogle(idToken)
    }

    private fun launchAuth(block: suspend () -> Unit) {
        if (_isWorking.value) return
        viewModelScope.launch {
            _isWorking.value = true
            _error.value = null
            runCatching { block() }
                .onFailure { _error.value = it.message ?: "Sign-in failed" }
            _isWorking.value = false
        }
    }
}
