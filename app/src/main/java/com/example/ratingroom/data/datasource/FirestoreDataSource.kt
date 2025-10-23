package com.example.ratingroom.data.datasource

import com.example.ratingroom.data.dtos.MovieDto
import com.example.ratingroom.data.dtos.NotificationDto
import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.data.dtos.UserDto
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

    suspend fun getUserProfile(): UserDto?
    suspend fun getUserProfileById(userId: String): UserDto?

    // ------- Amigos --------
    suspend fun getAllUsers(): List<UserDto>
    suspend fun followUser(targetUserId: String): Boolean
    suspend fun unfollowUser(targetUserId: String): Boolean
    suspend fun getFollowers(userId: String): List<UserDto>
    suspend fun getFollowing(userId: String): List<UserDto>

    // ------- Películas --------
    suspend fun getAllMovies(): List<MovieDto>
    suspend fun getMovieById(movieId: Int): MovieDto?
    suspend fun getMoviesByGenre(genre: String): List<MovieDto>
    suspend fun searchMovies(query: String): List<MovieDto>

    // ------- Reseñas --------
    suspend fun createReviewFanout(
        userId: String,
        movieId: Int,
        rating: Int,
        text: String
    ): String

    suspend fun getReviewsByMovie(movieId: Int): List<ReviewDto>
    suspend fun getReviewsByUser(userId: String): List<ReviewDto>
    fun observeReviewsByMovie(movieId: Int): Flow<List<ReviewDto>>
    fun observeReviewsByUser(userId: String): Flow<List<ReviewDto>>
    suspend fun sendOrDeleteLike(reviewId: String, userId: String): Boolean

    // ------- Notificaciones --------
    fun observeNotifications(userId: String): Flow<List<NotificationDto>>
    suspend fun markNotificationSeen(userId: String, notificationId: String)
    suspend fun markAllNotificationsSeen(userId: String)
}
