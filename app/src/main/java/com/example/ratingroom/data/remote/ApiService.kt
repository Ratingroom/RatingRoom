package com.example.ratingroom.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

data class PeliculaDto(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fechaSalida: String,
    val portada: String?
)

data class ReviewDto(
    val id: Int,
    val rating: Int,
    val texto: String,
    val usuario_id: Int,
    val pelicula_id: Int
)

data class ComentarioDto(
    val id: Int,
    val texto: String,
    val usuario_id: Int,
    val review_id: Int
)

interface ApiService {
    @GET("api/peliculas")
    suspend fun getPeliculas(): List<PeliculaDto>

    @GET("api/peliculas/{id}")
    suspend fun getPelicula(@Path("id") id: Int): PeliculaDto

    @GET("api/peliculas/{id}/reviews")
    suspend fun getReviewsDePelicula(@Path("id") id: Int): List<ReviewDto>

    @GET("api/reviews/{id}/comentarios")
    suspend fun getComentarios(@Path("id") id: Int): List<ComentarioDto>
}
