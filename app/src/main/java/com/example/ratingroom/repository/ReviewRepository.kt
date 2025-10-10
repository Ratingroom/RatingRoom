package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.dtos.ReviewDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val authRepository: AuthRepository
) {

    // ======== FIREBASE ========

    suspend fun create(currentUserId: Int, articuloId: Int, rating: Int, texto: String): ReviewDto? {
        val uid = authRepository.currentUser?.uid ?: return null
        val saved = firestoreDataSource.createReview(
            userId = uid,
            movieId = articuloId,
            rating = rating,
            text = texto
        )
        // Map a ReviewDto para no romper la UI ya existente
        return ReviewDto(
            id = (saved["createdAt"]?.hashCode() ?: (System.currentTimeMillis().toInt())),
            usuario_id = currentUserId,
            pelicula_id = articuloId,
            rating = rating,
            texto = texto
        )
    }

    suspend fun getReviewsByUser(movieId: Int, userId: Int): List<ReviewDto> {
        val uid = authRepository.currentUser?.uid ?: return emptyList()
        val list = firestoreDataSource.getReviewsByUser(uid)
        // Si te interesa filtrar por película:
        val filtered = if (movieId != 0) list.filter { (it["movieId"] as? Long)?.toInt() == movieId } else list
        return filtered.map {
            ReviewDto(
                id = (it["createdAt"]?.hashCode() ?: it.hashCode()),
                usuario_id = userId,
                pelicula_id = (it["movieId"] as? Long)?.toInt() ?: 0,
                rating = (it["rating"] as? Long)?.toInt() ?: 0,
                texto = it["text"] as? String ?: ""
            )
        }
    }

    suspend fun getReviewsByMovie(movieId: Int): List<ReviewDto> {
        val list = firestoreDataSource.getReviewsByMovie(movieId)
        return list.map {
            ReviewDto(
                id = (it["createdAt"]?.hashCode() ?: it.hashCode()),
                usuario_id = 0,
                pelicula_id = (it["movieId"] as? Long)?.toInt() ?: movieId,
                rating = (it["rating"] as? Long)?.toInt() ?: 0,
                texto = it["text"] as? String ?: ""
            )
        }
    }

    // Placeholders para compatibilidad con tu UI (si no los usas, quedan así)
    suspend fun update(currentUserId: Int, reviewId: Int, rating: Int, texto: String): ReviewDto? {
        // Implementación de update por docId String se puede agregar luego
        return null
    }

    suspend fun delete(currentUserId: Int, reviewId: Int): Boolean {
        // Implementación de delete por docId String se puede agregar luego
        return false
    }

    suspend fun listByUser(userId: Int): List<ReviewDto> {
        val uid = authRepository.currentUser?.uid ?: return emptyList()
        val list = firestoreDataSource.getReviewsByUser(uid)
        return list.map {
            ReviewDto(
                id = (it["createdAt"]?.hashCode() ?: it.hashCode()),
                usuario_id = userId,
                pelicula_id = (it["movieId"] as? Long)?.toInt() ?: 0,
                rating = (it["rating"] as? Long)?.toInt() ?: 0,
                texto = it["text"] as? String ?: ""
            )
        }
    }

    suspend fun getUserProfile(userId: Int) = null // ya no se usa desde REST
}
