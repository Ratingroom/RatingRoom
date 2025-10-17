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
class FriendsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUIState())
    val uiState: StateFlow<FriendsUIState> = _uiState.asStateFlow()

    init {
        loadFriendsData()
    }

    private fun loadFriendsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val followingRaw = FriendsRepository.getFriends()
                val suggestionsRaw = FriendsRepository.getSuggestions()
                val followersRaw = FriendsRepository.getFollowers()

                val followingIds = followingRaw.map { it.id }.toSet()
                val followersIds = followersRaw.map { it.id }.toSet()

                // Enriquecer con relationshipType según pertenencia en las listas
                val following = followingRaw.map { f ->
                    f.copy(
                        relationshipType = if (followersIds.contains(f.id)) FriendshipType.MUTUAL else FriendshipType.FOLLOWING,
                        isFollowing = true
                    )
                }

                val followers = followersRaw.map { f ->
                    val isMutual = followingIds.contains(f.id)
                    f.copy(
                        relationshipType = if (isMutual) FriendshipType.MUTUAL else FriendshipType.FOLLOWER,
                        isFollowing = isMutual
                    )
                }

                val suggestions = suggestionsRaw
                    .filter { it.id !in followingIds && it.id !in followersIds }
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

    fun onFriendAction(friendId: Int, action: String) {
        viewModelScope.launch {
            try {
                when (action) {
                    // Compatibilidad con versiones anteriores del UI
                    "add_friend" -> FriendsRepository.followUser(friendId)
                    "remove_friend" -> FriendsRepository.unfollowUser(friendId)
                    // Acciones principales
                    "follow" -> FriendsRepository.followUser(friendId)
                    "unfollow" -> FriendsRepository.unfollowUser(friendId)
                    "follow_back" -> FriendsRepository.followUser(friendId)
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