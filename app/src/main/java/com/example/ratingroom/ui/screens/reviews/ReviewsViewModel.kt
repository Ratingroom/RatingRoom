package com.example.ratingroom.ui.screens.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.services.ReviewApiService
import com.example.ratingroom.repository.MovieRepository
import com.example.ratingroom.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {
    
    // ID de usuario quemado para obtener datos de REST API
    private val HARDCODED_USER_ID = 2
    
    private val _uiState = MutableStateFlow(ReviewsUIState())
    val uiState: StateFlow<ReviewsUIState> = _uiState.asStateFlow()
    
    init {
        loadReviews()
    }
    
    private fun loadReviews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                println("ReviewsViewModel: Iniciando carga de reseñas para usuario $HARDCODED_USER_ID")
                
                val userReviews = reviewRepository.listByUser(HARDCODED_USER_ID)
                println("ReviewsViewModel: Reseñas obtenidas del backend: ${userReviews.size}")
                userReviews.forEach { review ->
                    println("ReviewsViewModel: Review ID=${review.id}, Rating=${review.rating}, PeliculaID=${review.pelicula_id}")
                }
                
                // Mapear las reseñas del backend a ReviewItem para la UI
                val reviews = mutableListOf<ReviewItem>()
                
                for (reviewDto in userReviews) {
                    try {
                        // Obtener información de la película (función suspend)
                        val movie = movieRepository.getMovieById(reviewDto.pelicula_id)
                        println("ReviewsViewModel: Película obtenida para ID ${reviewDto.pelicula_id}: ${movie?.title}")
                        
                        // Siempre agregar la reseña, incluso si no se encuentra la película
                        reviews.add(
                            ReviewItem(
                                id = reviewDto.id,
                                movieId = reviewDto.pelicula_id,
                                movieTitle = movie?.title ?: "Película desconocida (ID: ${reviewDto.pelicula_id})",
                                rating = reviewDto.rating,
                                comment = reviewDto.texto
                            )
                        )
                        println("ReviewsViewModel: ReviewItem agregado - Título: ${movie?.title ?: "Película ID ${reviewDto.pelicula_id}"}")
                    } catch (e: Exception) {
                        println("ReviewsViewModel: Error obteniendo película ID ${reviewDto.pelicula_id}: ${e.message}")
                        // Agregar la reseña con título genérico si hay error
                        reviews.add(
                            ReviewItem(
                                id = reviewDto.id,
                                movieId = reviewDto.pelicula_id,
                                movieTitle = "Película ID ${reviewDto.pelicula_id}",
                                rating = reviewDto.rating,
                                comment = reviewDto.texto
                            )
                        )
                        println("ReviewsViewModel: ReviewItem agregado con título genérico")
                    }
                }
                
                println("ReviewsViewModel: Total de ReviewItems creados: ${reviews.size}")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    reviews = reviews
                )
                
                println("ReviewsViewModel: Estado actualizado - isLoading=false, reviews.size=${reviews.size}")
            } catch (e: Exception) {
                println("ReviewsViewModel: Error al cargar reseñas: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun editReview(reviewId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            try {
                val updated = reviewRepository.update(HARDCODED_USER_ID, reviewId, rating, texto)
                if (updated != null) {
                    // Actualizar la reseña en la lista local
                    val updatedReviews = _uiState.value.reviews.map { review ->
                        if (review.id == reviewId) {
                            review.copy(rating = updated.rating, comment = updated.texto)
                        } else {
                            review
                        }
                    }
                    _uiState.value = _uiState.value.copy(reviews = updatedReviews)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            try {
                val success = reviewRepository.delete(HARDCODED_USER_ID, reviewId)
                if (success) {
                    // Remover la reseña de la lista local
                    val updatedReviews = _uiState.value.reviews.filter { it.id != reviewId }
                    _uiState.value = _uiState.value.copy(reviews = updatedReviews)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun refreshReviews() {
        loadReviews()
    }
}