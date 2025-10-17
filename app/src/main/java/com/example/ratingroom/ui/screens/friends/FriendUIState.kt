package com.example.ratingroom.ui.screens.friends

import com.example.ratingroom.ui.screens.profile.ProfileData

data class FriendUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profile: ProfileData? = null,
    val isFollowing: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val showFollowers: Boolean = false,
    val showFollowing: Boolean = false,
    val followersNames: List<String> = emptyList(),
    val followingNames: List<String> = emptyList()
)
