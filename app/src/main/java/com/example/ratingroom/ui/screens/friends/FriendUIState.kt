package com.example.ratingroom.ui.screens.friends

import com.example.ratingroom.data.dtos.ReviewDto

data class FriendProfileData(
    val uid: String = "",
    val name: String = "Usuario",
    val email: String = "",
    val memberSince: String? = null,
    val favoriteGenre: String? = null,
    val profileImageUrl: String? = null,
    val biography: String? = null,
    val location: String? = null,
    val website: String? = null
)

data class FriendUIState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val profile: FriendProfileData? = null,
    val isLoadingReviews: Boolean = false,
    val reviews: List<ReviewDto> = emptyList()
)
