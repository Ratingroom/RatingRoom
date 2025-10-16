package com.example.ratingroom.ui.screens.reviews

data class ReviewItem(
    val id: String,
    val movieId: Int,
    val movieTitle: String,
    val rating: Int,
    val comment: String,
    val likes: Int = 0,
    val isLiked: Boolean = false
)

data class ReviewsUIState(
    val reviews: List<ReviewItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)