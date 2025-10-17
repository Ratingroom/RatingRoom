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

    suspend fun create(currentUserId: Int, articuloId: Int, rating: Int, texto: String): ReviewDto? {
        val uid = authRepository.currentUser?.uid ?: return null

        val reviewId = firestoreDataSource.createReviewFanout(
            userId = uid,
            movieId = articuloId,
            rating = rating,
            text = texto
        )

        return ReviewDto(
            id = reviewId,
            usuario_id = currentUserId,
            pelicula_id = articuloId,
            rating = rating,
            texto = texto
        )
    }

    suspend fun getReviewsByMovie(movieId: Int): List<ReviewDto> {
        val list: List<Map<String, Any>> = firestoreDataSource.getReviewsByMovie(movieId)
        return mapReviewsFromFirestore(list, movieId)
    }
    
    fun observeReviewsByMovie(movieId: Int): Flow<List<ReviewDto>> {
        return firestoreDataSource.observeReviewsByMovie(movieId)
            .map { list -> mapReviewsFromFirestore(list, movieId) }
    }
    
    private fun mapReviewsFromFirestore(list: List<Map<String, Any>>, movieId: Int): List<ReviewDto> {
        return list.map { map ->
            val movieIdFromDb = (map["movieId"] as? Number)?.toInt() ?: movieId
            val ratingFromDb  = (map["rating"]  as? Number)?.toInt() ?: 0
            val textFromDb    = map["text"] as? String ?: ""
            val idStr         = (map["id"] as? String)
                ?: "${movieIdFromDb}_${textFromDb.hashCode()}_${ratingFromDb}"
            // 🎯 Información desnormalizada del usuario
            val userName = map["userName"] as? String
            val userImageUrl = map["userImageUrl"] as? String
            val likes = (map["likes"] as? Number)?.toInt() ?: 0

            ReviewDto(
                id = idStr, // ✅ Usar el ID real del documento (String)
                usuario_id = 0,
                pelicula_id = movieIdFromDb,
                rating = ratingFromDb,
                texto = textFromDb,
                userName = userName,
                userImageUrl = userImageUrl,
                likes = likes
            )
        }
    }

    suspend fun getReviewsByUser(movieId: Int, userId: Int): List<ReviewDto> {
        val uid = authRepository.currentUser?.uid ?: return emptyList()
        val list: List<Map<String, Any>> = firestoreDataSource.getReviewsByUser(uid)

        val filtered = if (movieId != 0)
            list.filter { (it["movieId"] as? Number)?.toInt() == movieId }
        else
            list

        return filtered.map { map ->
            val movieIdFromDb = (map["movieId"] as? Number)?.toInt() ?: 0
            val ratingFromDb  = (map["rating"]  as? Number)?.toInt() ?: 0
            val textFromDb    = map["text"] as? String ?: ""
            val idStr         = (map["id"] as? String)
                ?: "${movieIdFromDb}_${textFromDb.hashCode()}_${ratingFromDb}"
            // 🎯 Información desnormalizada del usuario
            val userName = map["userName"] as? String
            val userImageUrl = map["userImageUrl"] as? String
            val likes = (map["likes"] as? Number)?.toInt() ?: 0

            ReviewDto(
                id = idStr,
                usuario_id = userId,
                pelicula_id = movieIdFromDb,
                rating = ratingFromDb,
                texto = textFromDb,
                userName = userName,
                userImageUrl = userImageUrl,
                likes = likes
            )
        }
    }

    // ➕ NUEVO: obtener reseñas de cualquier usuario por su UID de Firestore (para FriendScreen)
    suspend fun getReviewsByUserUid(userUid: String): List<ReviewDto> {
        val list: List<Map<String, Any>> = firestoreDataSource.getReviewsByUser(userUid)
        return mapReviewsFromFirestore(list, 0)
    }
    
    fun observeReviewsByUserUid(userUid: String): Flow<List<ReviewDto>> {
        return firestoreDataSource.observeReviewsByUser(userUid)
            .map { list -> mapReviewsFromFirestore(list, 0) }
    }

    // Placeholders para compatibilidad si se usan desde la UI
    suspend fun update(currentUserId: Int, reviewId: String, rating: Int, texto: String): ReviewDto? = null
    suspend fun delete(currentUserId: Int, reviewId: String): Boolean = false
    suspend fun listByUser(userId: Int): List<ReviewDto> = getReviewsByUser(0, userId)
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
