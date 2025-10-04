
package com.example.ratingroom.data.remote

import retrofit2.http.*


data class ApiResponse<T>(val success: Boolean, val data: T?)
data class ReviewsResponse(
    val reviews: List<ReviewDto>,
    val pagination: PaginationDto
)
data class PaginationDto(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val itemsPerPage: Int
)
data class UserProfileDto(
    val id: Int,
    val nombre: String?,
    val username: String,
    val fotoPerfil: String?,
    val email: String?,
    val reviews: List<ReviewDto> = emptyList()
)

interface ReviewApi {
    @GET("api/usuarios/{id}") suspend fun getUserProfile(@Path("id") id: Int): ApiResponse<UserProfileDto>
    @GET("api/reviews") suspend fun getReviewsByUser(@Query("userId") userId: Int): ApiResponse<ReviewsResponse>
    @POST("api/reviews") suspend fun createReview(@Body body: Map<String, @JvmSuppressWildcards Any>, @Header("x-user-id") userIdHeader: Int): ApiResponse<ReviewDto>
    @PUT("api/reviews/{id}") suspend fun updateReview(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any>, @Header("x-user-id") userIdHeader: Int): ApiResponse<ReviewDto>
    @DELETE("api/reviews/{id}") suspend fun deleteReview(@Path("id") id: Int, @Header("x-user-id") userIdHeader: Int): ApiResponse<Unit>
}
