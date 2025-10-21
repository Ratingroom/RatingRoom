package com.example.ratingroom.data.dtos

data class MovieDto(
    val id: Int,
    val titulo: String = "",
    val title: String = "",
    val descripcion: String = "",
    val description: String = "",
    val fechaSalida: String = "",
    val year: String = "",
    val portada: String? = null,
    val imageUrl: String? = null,
    val genre: String = "",
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val director: String = "",
    val duration: String = ""
)

data class CreateMovieDto(
    val titulo: String,
    val descripcion: String,
    val fechaSalida: String,
    val portada: String?
)
