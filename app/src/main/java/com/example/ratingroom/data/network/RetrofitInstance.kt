package com.example.ratingroom.data.network

import com.example.ratingroom.data.services.CommentApiService
import com.example.ratingroom.data.services.MovieApiService
import com.example.ratingroom.data.services.ReviewApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // ✅ URL fija usando dominio local configurado en tu archivo hosts
    private const val BASE_URL = "http://ratingroom.local:3000/"

    // ✅ Instancia de Retrofit con Gson
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ✅ Servicios disponibles
    val reviewApi: ReviewApiService by lazy {
        retrofit.create(ReviewApiService::class.java)
    }

    val movieApi: MovieApiService by lazy {
        retrofit.create(MovieApiService::class.java)
    }

    val commentApi: CommentApiService by lazy {
        retrofit.create(CommentApiService::class.java)
    }
}
