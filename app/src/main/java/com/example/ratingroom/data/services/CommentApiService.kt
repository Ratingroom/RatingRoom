package com.example.ratingroom.data.services

import com.example.ratingroom.data.dtos.CommentDto
import retrofit2.http.GET
import retrofit2.http.Path

interface CommentApiService {
    @GET("api/reviews/{id}/comentarios")
    suspend fun getCommentsByReview(@Path("id") reviewId: Int): List<CommentDto>
}
