package com.example.ratingroom.ui.screens.friends

import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.ui.screens.profile.ProfileData

data class FriendUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    // Perfil del usuario visitado
    val profile: ProfileData? = null,

    // Seguir / contadores
    val isFollowing: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,

    // Diálogos
    val showFollowers: Boolean = false,
    val showFollowing: Boolean = false,
    val followersNames: List<String> = emptyList(),
    val followingNames: List<String> = emptyList(),

    // Reseñas del usuario visitado
    val isLoadingReviews: Boolean = false,
    val reviews: List<ReviewDto> = emptyList()
)
