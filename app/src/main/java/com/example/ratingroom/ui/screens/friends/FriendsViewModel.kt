package com.example.ratingroom.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.repository.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.ratingroom.data.models.Friend
import com.example.ratingroom.data.models.FriendshipType

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUIState())
    val uiState: StateFlow<FriendsUIState> = _uiState.asStateFlow()

    init {
        loadFriendsData()
    }

    private fun loadFriendsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val followingResult = friendsRepository.getFollowing()
                val followersResult = friendsRepository.getFollowers()
                val suggestionsResult = friendsRepository.getSuggestions()

                if (followingResult.isSuccess && followersResult.isSuccess && suggestionsResult.isSuccess) {
                    val followingRaw = followingResult.getOrNull() ?: emptyList()
                    val followersRaw = followersResult.getOrNull() ?: emptyList()
                    val suggestionsRaw = suggestionsResult.getOrNull() ?: emptyList()

                    val followingUids = followingRaw.mapNotNull { it.uid }.toSet()
                    val followersUids = followersRaw.mapNotNull { it.uid }.toSet()

                    val following = followingRaw.map { f ->
                        val isMutual = followersUids.contains(f.uid)
                        f.copy(
                            relationshipType = if (isMutual) FriendshipType.MUTUAL else FriendshipType.FOLLOWING,
                            isFollowing = true
                        )
                    }

                    val followers = followersRaw.map { f ->
                        val isMutual = followingUids.contains(f.uid)
                        f.copy(
                            relationshipType = if (isMutual) FriendshipType.MUTUAL else FriendshipType.FOLLOWER,
                            isFollowing = isMutual
                        )
                    }

                    val suggestions = suggestionsRaw
                        .filter { it.uid !in followingUids && it.uid !in followersUids }
                        .map { s ->
                            s.copy(
                                relationshipType = FriendshipType.NONE,
                                isFollowing = false,
                                isFriend = false
                            )
                        }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        friends = following,
                        suggestions = suggestions,
                        followers = followers
                    )
                } else {
                    val error = followingResult.exceptionOrNull() 
                        ?: followersResult.exceptionOrNull() 
                        ?: suggestionsResult.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error?.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // Implementar búsqueda
    }

    fun onTabSelected(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun onFriendAction(friend: Friend, action: String) {
        viewModelScope.launch {
            try {
                val targetUid = friend.uid
                if (targetUid.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(errorMessage = "UID de usuario no disponible")
                    return@launch
                }
                when (action) {
                    // Compatibilidad con versiones anteriores del UI
                    "add_friend" -> friendsRepository.followUser(targetUid)
                    "remove_friend" -> friendsRepository.unfollowUser(targetUid)
                    // Acciones principales
                    "follow" -> friendsRepository.followUser(targetUid)
                    "unfollow" -> friendsRepository.unfollowUser(targetUid)
                    "follow_back" -> friendsRepository.followUser(targetUid)
                }
                loadFriendsData() // Recargar datos
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}