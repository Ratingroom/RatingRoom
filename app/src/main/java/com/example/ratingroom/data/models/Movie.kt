package com.example.ratingroom.data.models

data class Movie(
    val id: Int,
    val title: String,
    val year: String,
    val genre: String,
    val rating: Double,
    val reviews: Int,
    val description: String,
    val director: String,
    val duration: String,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false
)

data class User(
    val id: Int,
    val displayName: String,
    val email: String,
    val biography: String = "",
    val location: String = "",
    val favoriteGenre: String = "",
    val birthdate: String = "",
    val website: String = "",
    val profileImageUrl: String? = null
)

data class Review(
    val id: Int,
    val movieId: Int,
    val userId: Int,
    val rating: Double,
    val comment: String,
    val date: String,
    // 🎯 Información del usuario autor del review
    val userName: String? = null,
    val userImageUrl: String? = null
)