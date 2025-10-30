package com.example.ratingroom.data.mappers

import com.example.ratingroom.data.models.Movie

// Modelo UI para Movie
data class MovieUi(
    val id: Int,
    val title: String,
    val year: String,
    val genre: String,
    val rating: Double,
    val reviews: Int,
    val description: String,
    val director: String,
    val duration: String,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false
)

fun Movie.toUi(): MovieUi = MovieUi(
    id = id,
    title = title,
    year = year,
    genre = genre,
    rating = rating,
    reviews = reviews,
    description = description,
    director = director,
    duration = duration,
    imageUrl = imageUrl,
    isFavorite = isFavorite
)

fun List<Movie>.toUiList(): List<MovieUi> = this.map { it.toUi() }