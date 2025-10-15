package com.example.ratingroom.data.services

import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.data.dtos.UserDto
import retrofit2.Response
import retrofit2.http.*

interface ReviewApiService {

    // Obtiene todas las reseñas de una película
    @GET("reviews/movie/{movieId}")
    suspend fun getReviewsByMovie(
        @Path("movieId") movieId: Int
    ): List<ReviewDto>

    // Obtiene todas las reseñas hechas por un usuario
    @GET("reviews/user/{userId}")
    suspend fun getReviewsByUser(
        @Path("userId") userId: Int
    ): List<ReviewDto>

    // Obtiene la información del perfil de un usuario
    @GET("users/{userId}")
    suspend fun getUserProfile(
        @Path("userId") userId: Int
    ): UserDto

    // Crea una nueva reseña
    @POST("reviews")
    suspend fun createReview(
        @Body review: ReviewDto
    ): ReviewDto

    // Actualiza una reseña existente
    @PUT("reviews/{reviewId}")
    suspend fun updateReview(
        @Path("reviewId") reviewId: Int,
        @Body review: ReviewDto
    ): ReviewDto

    // Elimina una reseña
    @DELETE("reviews/{reviewId}")
    suspend fun deleteReview(
        @Path("reviewId") reviewId: Int
    ): Response<Unit>
}
