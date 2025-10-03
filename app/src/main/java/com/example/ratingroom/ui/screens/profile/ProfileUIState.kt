package com.example.ratingroom.ui.screens.profile

import com.example.ratingroom.data.models.Review

data class ProfileData(
    val name: String,
    val email: String,
    val memberSince: String,
    val favoriteGenre: String,
    val reviewsCount: Int,
    val averageRating: Double,
    val profileImageUrl: String? = null
)

data class ProfileUIState(
    val profileData: ProfileData? = null,
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userReviews: List<Review> = emptyList(),
    val reviewsCount: Int = 0,
    val averageRating: Double = 0.0
)
