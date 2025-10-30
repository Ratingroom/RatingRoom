package com.example.ratingroom.data.mappers

import com.example.ratingroom.data.models.Review

// Modelo UI para Review
data class ReviewUi(
    val id: String,
    val movieId: Int,
    val userId: Int,
    val rating: Double,
    val comment: String,
    val date: String,
    val userName: String? = null,
    val userImageUrl: String? = null,
    val likes: Int = 0,
    val isLiked: Boolean = false
)

fun Review.toUi(): ReviewUi = ReviewUi(
    id = id,
    movieId = movieId,
    userId = userId,
    rating = rating,
    comment = comment,
    date = date,
    userName = userName,
    userImageUrl = userImageUrl,
    likes = likes,
    isLiked = isLiked
)

fun List<Review>.toUiList(): List<ReviewUi> = this.map { it.toUi() }