package com.example.ratingroom.data.datasource

import kotlinx.coroutines.flow.Flow

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
    suspend fun getUserProfileById(userId: String): Map<String, Any>?

    // ------- Amigos (Seguidores/Seguidos) --------
    /** Lista de todos los usuarios (para sugerencias) */
    suspend fun getAllUsers(): List<Map<String, Any>>

    /** Seguir / dejar de seguir */
    suspend fun followUser(targetUserId: String): Boolean
    suspend fun unfollowUser(targetUserId: String): Boolean

    /** Listas de seguidores y seguidos */
    suspend fun getFollowers(userId: String): List<Map<String, Any>>
    suspend fun getFollowing(userId: String): List<Map<String, Any>>

    // ------- Películas --------
    suspend fun getAllMovies(): List<Map<String, Any>>
    suspend fun getMovieById(movieId: Int): Map<String, Any>?
    suspend fun getMoviesByGenre(genre: String): List<Map<String, Any>>
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

    /** Lecturas únicas */
    suspend fun getReviewsByMovie(movieId: Int): List<Map<String, Any>>
    suspend fun getReviewsByUser(userId: String): List<Map<String, Any>>

    /** Tiempo real */
    fun observeReviewsByMovie(movieId: Int): Flow<List<Map<String, Any>>>
    fun observeReviewsByUser(userId: String): Flow<List<Map<String, Any>>>

    /** Like toggle en reseña: true si quedó con like, false si se quitó */
    suspend fun sendOrDeleteLike(reviewId: String, userId: String): Boolean
}
