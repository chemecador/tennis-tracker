package com.chemecador.tennistracker.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.tennistracker.data.friends.Friendship
import com.chemecador.tennistracker.data.friends.FriendshipAlreadyExistsException
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

data class FriendItem(
    val friendship: Friendship,
    val other: UserProfile?,
)

data class FriendsUiState(
    val incoming: List<FriendItem> = emptyList(),
    val outgoing: List<FriendItem> = emptyList(),
    val accepted: List<FriendItem> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface AddFriendResult {
    data object Idle : AddFriendResult
    data object Searching : AddFriendResult
    data object NotFound : AddFriendResult
    data object SelfNotAllowed : AddFriendResult
    data class Found(val profile: UserProfile) : AddFriendResult
    data object Sending : AddFriendResult
    data object Sent : AddFriendResult
    data object AlreadyExists : AddFriendResult
    data class Error(val message: String) : AddFriendResult
}

class FriendsViewModel(
    private val myUid: String,
    private val friendshipRepo: FriendshipRepository,
    private val profileRepo: UserProfileRepository,
) : ViewModel() {

    private val profileCache = mutableMapOf<String, UserProfile?>()

    val uiState: StateFlow<FriendsUiState> = friendshipRepo.observeFriendships(myUid)
        .onStart { emit(emptyList()) }
        .catch { emit(emptyList()) }
        .map { friendships ->
            val items = friendships.map { f ->
                val otherUid = f.otherUid(myUid)
                val other = profileCache.getOrPut(otherUid) {
                    runCatching { profileRepo.getProfileOnce(otherUid) }.getOrNull()
                }
                FriendItem(friendship = f, other = other)
            }
            FriendsUiState(
                incoming = items.filter {
                    it.friendship.status == FriendshipStatus.PENDING &&
                        it.friendship.requestedBy != myUid
                },
                outgoing = items.filter {
                    it.friendship.status == FriendshipStatus.PENDING &&
                        it.friendship.requestedBy == myUid
                },
                accepted = items.filter { it.friendship.status == FriendshipStatus.ACCEPTED },
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState())

    private val _addResult = MutableStateFlow<AddFriendResult>(AddFriendResult.Idle)
    val addResult: StateFlow<AddFriendResult> = _addResult.asStateFlow()

    fun searchUsername(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            _addResult.value = AddFriendResult.Idle
            return
        }
        _addResult.value = AddFriendResult.Searching
        viewModelScope.launch {
            val profile = runCatching { profileRepo.findUserByUsername(trimmed) }
                .getOrElse {
                    _addResult.value = AddFriendResult.Error(it.message ?: "Search failed")
                    return@launch
                }
            _addResult.value = when {
                profile == null -> AddFriendResult.NotFound
                profile.uid == myUid -> AddFriendResult.SelfNotAllowed
                else -> AddFriendResult.Found(profile)
            }
        }
    }

    fun sendRequest(toUid: String) {
        _addResult.value = AddFriendResult.Sending
        viewModelScope.launch {
            runCatching { friendshipRepo.sendRequest(myUid, toUid) }
                .onSuccess { _addResult.value = AddFriendResult.Sent }
                .onFailure { throwable ->
                    _addResult.value = when (throwable) {
                        is FriendshipAlreadyExistsException -> AddFriendResult.AlreadyExists
                        else -> AddFriendResult.Error(throwable.message ?: "Could not send request")
                    }
                }
        }
    }

    fun resetAddResult() {
        _addResult.value = AddFriendResult.Idle
    }

    fun accept(friendshipId: String) {
        viewModelScope.launch {
            runCatching { friendshipRepo.acceptRequest(friendshipId) }
        }
    }

    fun delete(friendshipId: String) {
        viewModelScope.launch {
            runCatching { friendshipRepo.deleteFriendship(friendshipId) }
        }
    }
}
