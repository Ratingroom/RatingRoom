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
        profileImageUrl: String? = null,
        mainMovieId: Int? = null
    )

    suspend fun getUserProfile(): Map<String, Any>?

    // ------- Películas --------
    /** Obtiene todas las películas desde Firebase */
    suspend fun getAllMovies(): List<Map<String, Any>>

    /** Obtiene una película específica por ID desde Firebase */
    suspend fun getMovieById(movieId: Int): Map<String, Any>?

    /** Obtiene películas por género desde Firebase */
    suspend fun getMoviesByGenre(genre: String): List<Map<String, Any>>

    /** Busca películas por query desde Firebase */
    suspend fun searchMovies(query: String): List<Map<String, Any>>

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

    suspend fun sendOrDeleteLike(reviewId: String, userId: String): Boolean
}
