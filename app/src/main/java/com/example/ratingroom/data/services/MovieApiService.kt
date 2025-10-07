package com.example.ratingroom.data.services

import com.example.ratingroom.data.dtos.MovieDto
import retrofit2.http.GET
import retrofit2.http.Path

interface MovieApiService {
    @GET("api/peliculas")
    suspend fun getMovies(): List<MovieDto>

    @GET("api/peliculas/{id}")
    suspend fun getMovie(@Path("id") id: Int): MovieDto
}
