package com.example.ratingroom.injection

import com.example.ratingroom.data.datasource.AuthRemoteDataSource
import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.datasource.impl.AuthRemoteDataSourceImpl
import com.example.ratingroom.data.datasource.impl.FirestoreDataSourceImpl
import com.example.ratingroom.repository.MovieFirebaseRepository
import com.example.ratingroom.repository.MovieRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(
        authService: FirebaseAuth
    ): AuthRemoteDataSource {
        return AuthRemoteDataSourceImpl(authService)
    }

    @Provides
    @Singleton
    fun provideFirestoreDataSource(
        firestoreService: FirebaseFirestore,
        authService: FirebaseAuth
    ): FirestoreDataSource {
        return FirestoreDataSourceImpl(firestoreService, authService)
    }

    @Provides
    @Singleton
    fun provideMovieFirebaseRepository(
        firestoreDataSource: FirestoreDataSource
    ): MovieFirebaseRepository {
        return MovieFirebaseRepository(firestoreDataSource)
    }

    @Provides
    @Singleton
    fun provideMovieRepository(
        movieFirebaseRepository: MovieFirebaseRepository,
        authRepository: com.example.ratingroom.repository.AuthRepository
    ): MovieRepository {
        return MovieRepository(movieFirebaseRepository, authRepository)
    }
}