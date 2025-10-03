package com.example.ratingroom.data.remote

import com.example.ratingroom.data.models.ApiResponse
import com.example.ratingroom.data.models.Movie
import retrofit2.http.GET

interface MovieApiService {
    @GET("peliculas")
    suspend fun getMovies(): ApiResponse<List<Movie>>
}
