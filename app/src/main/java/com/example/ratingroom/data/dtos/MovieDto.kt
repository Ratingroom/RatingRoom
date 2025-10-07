package com.example.ratingroom.data.dtos

data class MovieDto(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fechaSalida: String,
    val portada: String?
)

data class CreateMovieDto(
    val titulo: String,
    val descripcion: String,
    val fechaSalida: String,
    val portada: String?
)
