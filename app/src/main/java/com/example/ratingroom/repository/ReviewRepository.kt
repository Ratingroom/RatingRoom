package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.dtos.ReviewDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    private fun mapReviewFromFirestore(map: Map<String, Any>): ReviewDto {
        val id = (map["id"] as? String) ?: ""
        val movieId = (map["movieId"] as? Number)?.toInt() ?: (map["pelicula_id"] as? Number)?.toInt() ?: 0
        val userIdInt = when (val uidVal = map["userId"]) {
            is Number -> uidVal.toInt()
            is String -> uidVal.toIntOrNull() ?: 0
            else -> (map["usuario_id"] as? Number)?.toInt() ?: 0
        }
        val rating = (map["rating"] as? Number)?.toInt() ?: 0
        val texto = (map["text"] as? String) ?: (map["texto"] as? String) ?: ""
        val userName = map["userName"] as? String
        val userImageUrl = map["userImageUrl"] as? String
        val likes = (map["likes"] as? Number)?.toInt() ?: 0
        return ReviewDto(
            id = id,
            rating = rating,
            texto = texto,
            usuario_id = userIdInt,
            pelicula_id = movieId,
            userName = userName,
            userImageUrl = userImageUrl,
            likes = likes
        )
    }

    suspend fun getReviewsByMovie(movieId: Int): Result<List<ReviewDto>> {
        return try {
            val maps = firestoreDataSource.getReviewsByMovie(movieId)
            val dtos = maps.map { mapReviewFromFirestore(it) }
            Result.success(dtos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeReviewsByMovie(movieId: Int): Flow<List<ReviewDto>> {
        return firestoreDataSource.observeReviewsByMovie(movieId)
            .map { list -> list.map { mapReviewFromFirestore(it) } }
    }

    suspend fun getReviewsByUser(movieId: Int, userId: Int): Result<List<ReviewDto>> {
        return try {
            val uid = authRepository.currentUser?.uid 
                ?: throw IllegalStateException("Usuario no autenticado")
            val maps = firestoreDataSource.getReviewsByUser(uid)

            val filteredMaps = if (movieId != 0)
                maps.filter { ((it["movieId"] as? Number)?.toInt() ?: 0) == movieId }
            else
                maps

            val dtos = filteredMaps.map { mapReviewFromFirestore(it) }
            Result.success(dtos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviewsByUserUid(userUid: String): Result<List<ReviewDto>> {
        return try {
            val maps = firestoreDataSource.getReviewsByUser(userUid)
            val dtos = maps.map { mapReviewFromFirestore(it) }
            Result.success(dtos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeReviewsByUserUid(userUid: String): Flow<List<ReviewDto>> {
        return firestoreDataSource.observeReviewsByUser(userUid)
            .map { list -> list.map { mapReviewFromFirestore(it) } }
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
