package com.example.ratingroom.injection

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.repository.ReviewRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideReviewRepository(
        firestoreDataSource: FirestoreDataSource,
        authRepository: AuthRepository
    ): ReviewRepository {
        return ReviewRepository(
            firestoreDataSource = firestoreDataSource,
            authRepository = authRepository
        )
    }
}
