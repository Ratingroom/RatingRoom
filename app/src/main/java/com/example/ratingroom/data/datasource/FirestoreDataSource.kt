package com.example.ratingroom.data.datasource

interface FirestoreDataSource {

    // ------- Perfil --------
    suspend fun createUserDocument(
        userId: String,
        email: String,
        fullName: String?,
        favoriteGenre: String?,
        birthYear: String?
    )

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

    // ------- Reseñas --------
    /**
     * Crea una reseña y hace fanout:
     * - /reviews/{reviewId}
     * - /users/{userId}/reviews/{reviewId}
     * - /movies/{movieId}/reviews/{reviewId}
     *
     * @return reviewId generado
     */
    suspend fun createReviewFanout(
        userId: String,
        movieId: Int,
        rating: Int,
        text: String
    ): String

    /** Lee reseñas por película (de /movies/{movieId}/reviews) */
    suspend fun getReviewsByMovie(movieId: Int): List<Map<String, Any>>

    /** Obtiene el perfil de un usuario específico por su UID */
    suspend fun getUserProfileById(userId: String): Map<String, Any>?

    /** Lee reseñas del usuario (de /users/{userId}/reviews) */
    suspend fun getReviewsByUser(userId: String): List<Map<String, Any>>
}
