package com.example.ratingroom.ui.screens.moviedetail

import com.example.ratingroom.data.models.Movie
import com.example.ratingroom.data.models.Review

data class MovieDetailUIState(
    val isLoading: Boolean = false,
    val movie: Movie? = null,
    val reviews: List<Review> = emptyList(),
    val errorMessage: String? = null,
    val showFollowingOnly: Boolean = false,
    val followingNames: Set<String> = emptySet(),
    val likedIds: Set<String> = emptySet()
)