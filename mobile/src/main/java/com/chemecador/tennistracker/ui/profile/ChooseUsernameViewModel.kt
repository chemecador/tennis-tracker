package com.chemecador.tennistracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.tennistracker.data.profile.InvalidUsernameException
import com.chemecador.tennistracker.data.profile.UserProfileRepository
import com.chemecador.tennistracker.data.profile.UserProfileRepository.Companion.RESERVED_USERNAMES
import com.chemecador.tennistracker.data.profile.UserProfileRepository.Companion.isValidUsername
import com.chemecador.tennistracker.data.profile.UserProfileRepository.Companion.normalizeUsername
import com.chemecador.tennistracker.data.profile.UsernameTakenException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UsernameValidation {
    data object Empty : UsernameValidation
    data object BadFormat : UsernameValidation
    data object Reserved : UsernameValidation
    data object Checking : UsernameValidation
    data object Available : UsernameValidation
    data object Taken : UsernameValidation
}

class ChooseUsernameViewModel(
    private val repository: UserProfileRepository = UserProfileRepository(),
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _validation = MutableStateFlow<UsernameValidation>(UsernameValidation.Empty)
    val validation: StateFlow<UsernameValidation> = _validation.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var availabilityJob: Job? = null

    fun onUsernameChange(input: String) {
        _username.value = input
        _error.value = null
        availabilityJob?.cancel()

        val normalized = input.normalizeUsername()
        when {
            normalized.isEmpty() -> _validation.value = UsernameValidation.Empty
            !normalized.isValidUsername() -> _validation.value = UsernameValidation.BadFormat
            normalized in RESERVED_USERNAMES -> _validation.value = UsernameValidation.Reserved
            else -> {
                _validation.value = UsernameValidation.Checking
                availabilityJob = viewModelScope.launch {
                    delay(AVAILABILITY_DEBOUNCE_MS)
                    val isAvailable = runCatching { repository.isUsernameAvailable(normalized) }
                        .getOrElse { return@launch }
                    if (_username.value.normalizeUsername() != normalized) return@launch
                    _validation.value =
                        if (isAvailable) UsernameValidation.Available else UsernameValidation.Taken
                }
            }
        }
    }

    fun submit(uid: String) {
        if (_isSubmitting.value) return
        if (_validation.value !is UsernameValidation.Available) return
        val normalized = _username.value.normalizeUsername()
        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            runCatching {
                repository.claimUsernameAndCreateProfile(
                    uid = uid,
                    username = normalized,
                    displayName = normalized,
                )
            }.onFailure { throwable ->
                when (throwable) {
                    is UsernameTakenException -> _validation.value = UsernameValidation.Taken
                    is InvalidUsernameException -> _validation.value = UsernameValidation.BadFormat
                    else -> _error.value = throwable.message ?: "Could not create profile"
                }
            }
            _isSubmitting.value = false
        }
    }

    companion object {
        private const val AVAILABILITY_DEBOUNCE_MS = 300L
    }
}
