package com.example.ratingroom.data.dtos

data class ReviewDto(
    val id: Int,
    val rating: Int,
    val texto: String,
    val usuario_id: Int,
    val pelicula_id: Int
)

data class CreateReviewDto(
    val rating: Int,
    val texto: String,
    val pelicula_id: Int
)
