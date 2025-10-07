package com.example.ratingroom.repository

import com.example.ratingroom.data.services.ReviewApiService
import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.data.dtos.UserDto

class ReviewRepository(private val api: ReviewApiService) {
    
    suspend fun getReviewsByMovie(movieId: Int): List<ReviewDto> {
        return api.getReviewsByMovie(movieId)
    }
    
    suspend fun getReviewsByUser(movieId: Int, userId: Int): List<ReviewDto> {
        // Filtrar las reseñas para mostrar solo las del usuario especificado
        return api.getReviewsByMovie(movieId).filter { it.usuario_id == userId }
    }
    
    // Método para obtener todas las reseñas de un usuario (usando todas las películas)
    suspend fun listByUser(userId: Int): List<ReviewDto> {
        // TODO: Implementar endpoint específico para obtener reseñas por usuario
        // Por ahora retornamos lista vacía hasta tener el endpoint
        return emptyList()
    }
    
    // Método para obtener perfil de usuario (placeholder)
    suspend fun getUserProfile(userId: Int): UserDto? {
        // TODO: Implementar cuando tengamos el servicio de usuarios
        return null
    }
    
    // Método para crear reseña (placeholder)
    suspend fun create(currentUserId: Int, articuloId: Int, rating: Int, texto: String): ReviewDto? {
        // TODO: Implementar cuando tengamos el endpoint POST
        return null
    }
    
    // Método para actualizar reseña (placeholder)
    suspend fun update(currentUserId: Int, reviewId: Int, rating: Int, texto: String): ReviewDto? {
        // TODO: Implementar cuando tengamos el endpoint PUT
        return null
    }
    
    // Método para eliminar reseña (placeholder)
    suspend fun delete(currentUserId: Int, reviewId: Int): Boolean {
        // TODO: Implementar cuando tengamos el endpoint DELETE
        return false
    }
}
