package com.chemecador.tennistracker.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.tennistracker.data.friends.FriendshipRepository
import com.chemecador.tennistracker.data.friends.FriendshipStatus
import com.chemecador.tennistracker.data.profile.UserProfile
import com.chemecador.tennistracker.data.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OpponentSearchResult {
    data object Idle : OpponentSearchResult
    data object Searching : OpponentSearchResult
    data object NotFound : OpponentSearchResult
    data object SelfNotAllowed : OpponentSearchResult
    data class Found(val profile: UserProfile) : OpponentSearchResult
    data class Error(val message: String) : OpponentSearchResult
}

class SetupMatchViewModel(
    private val myUid: String,
    private val profileRepo: UserProfileRepository,
    friendshipRepo: FriendshipRepository,
) : ViewModel() {

    private val profileCache = mutableMapOf<String, UserProfile?>()

    val friends: StateFlow<List<UserProfile>> = friendshipRepo.observeFriendships(myUid)
        .onStart { emit(emptyList()) }
        .catch { emit(emptyList()) }
        .map { friendships ->
            friendships
                .filter { it.status == FriendshipStatus.ACCEPTED }
                .mapNotNull { f ->
                    val otherUid = f.otherUid(myUid)
                    profileCache.getOrPut(otherUid) {
                        runCatching { profileRepo.getProfileOnce(otherUid) }.getOrNull()
                    }
                }
                .sortedBy { it.username }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchResult = MutableStateFlow<OpponentSearchResult>(OpponentSearchResult.Idle)
    val searchResult: StateFlow<OpponentSearchResult> = _searchResult.asStateFlow()

    fun search(username: String) {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) {
            _searchResult.value = OpponentSearchResult.Idle
            return
        }
        _searchResult.value = OpponentSearchResult.Searching
        viewModelScope.launch {
            val profile = runCatching { profileRepo.findUserByUsername(trimmed) }
                .getOrElse {
                    _searchResult.value =
                        OpponentSearchResult.Error(it.message ?: "Search failed")
                    return@launch
                }
            _searchResult.value = when {
                profile == null -> OpponentSearchResult.NotFound
                profile.uid == myUid -> OpponentSearchResult.SelfNotAllowed
                else -> OpponentSearchResult.Found(profile)
            }
        }
    }

    fun resetSearch() {
        _searchResult.value = OpponentSearchResult.Idle
    }
}
