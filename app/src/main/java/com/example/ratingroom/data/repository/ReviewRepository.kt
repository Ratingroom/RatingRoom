package com.example.ratingroom.data.repository

import com.example.ratingroom.data.remote.ReviewApi
import com.example.ratingroom.data.remote.ReviewDto

class ReviewRepository(private val api: ReviewApi) {
    suspend fun getUserProfile(userId: Int) = api.getUserProfile(userId).data
    suspend fun listByUser(userId: Int): List<ReviewDto> {
        val response = api.getReviewsByUser(userId).data?.reviews ?: emptyList()
        // Filtrar las reseñas para mostrar solo las del usuario especificado
        return response.filter { it.usuario_id == userId }
    }
    suspend fun create(currentUserId: Int, articuloId: Int, rating: Int, texto: String) =
        api.createReview(mapOf("pelicula_id" to articuloId, "usuario_id" to currentUserId, "rating" to rating, "texto" to texto), currentUserId).data
    suspend fun update(currentUserId: Int, reviewId: Int, rating: Int, texto: String) =
        api.updateReview(reviewId, mapOf("rating" to rating, "texto" to texto), currentUserId).data
    suspend fun delete(currentUserId: Int, reviewId: Int) =
        api.deleteReview(reviewId, currentUserId).success
}
