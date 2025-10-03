package com.example.ratingroom.data.repository

import com.example.ratingroom.data.remote.ReviewApi

class ReviewRepository(private val api: ReviewApi) {
    suspend fun getUserProfile(userId: Int) = api.getUserProfile(userId).data
    suspend fun listByUser(userId: Int) = api.getReviewsByUser(userId).data ?: emptyList()
    suspend fun create(currentUserId: Int, articuloId: Int, rating: Int, texto: String) =
        api.createReview(mapOf("articuloId" to articuloId, "rating" to rating, "texto" to texto), currentUserId).data
    suspend fun update(currentUserId: Int, reviewId: Int, rating: Int, texto: String) =
        api.updateReview(reviewId, mapOf("rating" to rating, "texto" to texto), currentUserId).data
    suspend fun delete(currentUserId: Int, reviewId: Int) =
        api.deleteReview(reviewId, currentUserId).success
}
