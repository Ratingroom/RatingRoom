
package com.example.ratingroom.data.remote

import retrofit2.http.*

data class ApiResponse<T>(val success: Boolean, val data: T?)
data class ReviewDto(val id: Int, val usuario_id: Int, val pelicula_id: Int, val rating: Int, val texto: String)
data class UserProfileDto(
    val id: Int,
    val nombre: String?,
    val usuario: String,
    val fotoPerfil: String?,
    val email: String?,
    val reviews: List<ReviewDto> = emptyList()
)

interface ReviewApi {
    @GET("users/{id}") suspend fun getUserProfile(@Path("id") id: Int): ApiResponse<UserProfileDto>
    @GET("reviews") suspend fun getReviewsByUser(@Query("userId") userId: Int): ApiResponse<List<ReviewDto>>
    @POST("reviews") suspend fun createReview(@Body body: Map<String, @JvmSuppressWildcards Any>, @Header("x-user-id") userIdHeader: Int): ApiResponse<ReviewDto>
    @PUT("reviews/{id}") suspend fun updateReview(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any>, @Header("x-user-id") userIdHeader: Int): ApiResponse<ReviewDto>
    @DELETE("reviews/{id}") suspend fun deleteReview(@Path("id") id: Int, @Header("x-user-id") userIdHeader: Int): ApiResponse<Unit>
}
