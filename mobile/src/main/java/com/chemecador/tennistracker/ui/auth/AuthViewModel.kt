package com.chemecador.tennistracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.tennistracker.auth.AuthRepository
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

    fun signIn(email: String, password: String) {
        if (!validate(email, password)) return
        launchAuth { repository.signIn(email.trim(), password) }
    }

    fun register(email: String, password: String) {
        if (!validate(email, password)) return
        launchAuth { repository.register(email.trim(), password) }
    }

    fun signInAsGuest() = launchAuth { repository.signInAnonymously() }

    fun signInWithGoogle(idToken: String) = launchAuth { repository.signInWithGoogle(idToken) }

    fun onAuthError(message: String) {
        _error.value = message
    }

    fun signOut() = repository.signOut()

    fun clearError() {
        _error.value = null
    }

    private fun validate(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _error.value = "Introduce un email."
            return false
        }
        if (password.length < 6) {
            _error.value = "La contraseña debe tener al menos 6 caracteres."
            return false
        }
        return true
    }

    private fun launchAuth(block: suspend () -> FirebaseUser) {
        if (_isWorking.value) return
        viewModelScope.launch {
            _isWorking.value = true
            _error.value = null
            runCatching { block() }
                .onFailure { _error.value = it.message ?: "Authentication failed" }
            _isWorking.value = false
        }
    }
}
