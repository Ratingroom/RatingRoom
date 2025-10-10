package com.example.ratingroom.injection

import com.example.ratingroom.data.network.RetrofitInstance
import com.example.ratingroom.data.services.ReviewApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Binding con QUALIFIER para evitar duplicados
    @Provides
    @Singleton
    @Named("rr")
    fun provideReviewApi(): ReviewApiService = RetrofitInstance.reviewApi
}
