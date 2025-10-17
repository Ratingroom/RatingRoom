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
    suspend fun getUserProfileById(userId: String): Map<String, Any>?

    // ------- Amigos (Seguidores/Seguidos) --------
    /** Lista de usuarios que sigue el usuario actual */
    suspend fun getFollowing(userId: String): List<Map<String, Any>>

    /** Lista de usuarios que siguen al usuario actual */
    suspend fun getFollowers(userId: String): List<Map<String, Any>>

    /** Lista de todos los usuarios (para generar sugerencias) */
    suspend fun getAllUsers(): List<Map<String, Any>>

    /** Seguir a un usuario (crea documentos en following y followers) */
    suspend fun followUser(targetUserId: String)

    /** Dejar de seguir a un usuario (elimina documentos en following y followers) */
    suspend fun unfollowUser(targetUserId: String)

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

    suspend fun getReviewsByMovie(movieId: Int): List<Map<String, Any>>
    suspend fun getReviewsByUser(userId: String): List<Map<String, Any>>

    /** Envía o elimina like en una reseña. Devuelve true si quedó con like, false si se eliminó */
    suspend fun sendOrDeleteLike(reviewId: String, userId: String): Boolean
}
