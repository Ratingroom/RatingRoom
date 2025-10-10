package com.example.ratingroom.data.services

import com.example.ratingroom.data.dtos.ReviewDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ReviewApiService {
    @GET("api/peliculas/{id}/reviews")
    suspend fun getReviewsByMovie(@Path("id") movieId: Int): List<ReviewDto>
}
