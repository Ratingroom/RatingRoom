package com.example.ratingroom.ui.screens.moviedetail

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
class MovieDetailViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    
    // ID de usuario quemado para obtener datos de REST API
    private val HARDCODED_USER_ID = 2
    
    private val _uiState = MutableStateFlow(MovieDetailUIState())
    val uiState: StateFlow<MovieDetailUIState> = _uiState.asStateFlow()
    
    fun loadMovieDetail(movieId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val movie = MovieRepository.getMovieById(movieId)
                val reviews = MovieRepository.getReviewsForMovie(movieId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movie = movie,
                    reviews = reviews
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun createReview(movieId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            try {
                val created = reviewRepository.create(HARDCODED_USER_ID, movieId, rating, texto)
                if (created != null) {
                    // Recargar las reseñas de la película para mostrar la nueva reseña
                    loadMovieDetail(movieId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
}