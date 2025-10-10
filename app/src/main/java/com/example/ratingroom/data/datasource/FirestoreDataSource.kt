package com.example.ratingroom.data.datasource

interface FirestoreDataSource {
    // ===== Perfil (ya existentes) =====
    suspend fun updateUserProfile(
        displayName: String? = null,
        email: String? = null,
        biography: String? = null,
        location: String? = null,
        favoriteGenre: String? = null,
        birthdate: String? = null,
        website: String? = null,
        profileImageUrl: String? = null
    )

    suspend fun getUserProfile(): Map<String, Any>?

    suspend fun createUserDocument(
        userId: String,
        email: String,
        fullName: String?,
        favoriteGenre: String?,
        birthYear: String?
    )

    // ===== Reseñas (nuevo) =====
    suspend fun createReview(
        userId: String,
        movieId: Int,
        rating: Int,
        text: String
    ): Map<String, Any>

    suspend fun getReviewsByUser(userId: String): List<Map<String, Any>>

    suspend fun getReviewsByMovie(movieId: Int): List<Map<String, Any>>
}
