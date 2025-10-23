package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.dtos.ReviewDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val authRepository: AuthRepository
) {

    suspend fun create(currentUserId: Int, articuloId: Int, rating: Int, texto: String): Result<ReviewDto> {
        return try {
            val uid = authRepository.currentUser?.uid 
                ?: throw IllegalStateException("Usuario no autenticado")

            val reviewId = firestoreDataSource.createReviewFanout(
                userId = uid,
                movieId = articuloId,
                rating = rating,
                text = texto
            )

            Result.success(ReviewDto(
                id = reviewId,
                usuario_id = currentUserId,
                pelicula_id = articuloId,
                rating = rating,
                texto = texto
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviewsByMovie(movieId: Int): Result<List<ReviewDto>> {
        return try {
            val list = firestoreDataSource.getReviewsByMovie(movieId)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeReviewsByMovie(movieId: Int): Flow<List<ReviewDto>> {
        return firestoreDataSource.observeReviewsByMovie(movieId)
    }

    suspend fun getReviewsByUser(movieId: Int, userId: Int): Result<List<ReviewDto>> {
        return try {
            val uid = authRepository.currentUser?.uid 
                ?: throw IllegalStateException("Usuario no autenticado")
            val list = firestoreDataSource.getReviewsByUser(uid)

            val filtered = if (movieId != 0)
                list.filter { it.pelicula_id == movieId }
            else
                list

            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviewsByUserUid(userUid: String): Result<List<ReviewDto>> {
        return try {
            val list = firestoreDataSource.getReviewsByUser(userUid)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeReviewsByUserUid(userUid: String): Flow<List<ReviewDto>> {
        return firestoreDataSource.observeReviewsByUser(userUid)
    }

    suspend fun update(currentUserId: Int, reviewId: String, rating: Int, texto: String): Result<ReviewDto?> = 
        Result.success(null)
    
    suspend fun delete(currentUserId: Int, reviewId: String): Result<Boolean> = 
        Result.success(false)
    
    suspend fun listByUser(userId: Int): Result<List<ReviewDto>> = getReviewsByUser(0, userId)
    
    suspend fun getUserProfile(userId: Int) = null

    suspend fun sendOrDeleteLike(reviewId: String, userId: String): Result<Boolean> {
        return try {
            val wasLiked = firestoreDataSource.sendOrDeleteLike(reviewId, userId)
            Result.success(wasLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
