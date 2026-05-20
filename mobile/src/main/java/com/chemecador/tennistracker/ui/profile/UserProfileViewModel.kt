package com.chemecador.tennistracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.tennistracker.data.profile.UserProfile
import com.chemecador.tennistracker.data.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class UserProfileViewModel(
    uid: String,
    repository: UserProfileRepository = UserProfileRepository(),
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        repository.observeProfile(uid)
            .onEach {
                _profile.value = it
                _isLoading.value = false
            }
            .catch { _isLoading.value = false }
            .launchIn(viewModelScope)
    }
}
