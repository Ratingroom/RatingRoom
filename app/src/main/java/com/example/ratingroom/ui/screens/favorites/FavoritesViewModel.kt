package com.example.ratingroom.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.models.Movie
import com.example.ratingroom.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val authRepository: com.example.ratingroom.repository.AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FavoritesUIState())
    val uiState: StateFlow<FavoritesUIState> = _uiState.asStateFlow()
    
    init {
        loadFavorites()
    }
    
    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val favoriteMovies = movieRepository.getFavoriteMovies()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    favoriteMovies = favoriteMovies
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun removeFromFavorites(movie: Movie) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _uiState.value = _uiState.value.copy(errorMessage = "Debes iniciar sesión")
                    return@launch
                }
                
                // Toggle favorito en Firebase (lo quitará porque ya está marcado)
                val result = movieRepository.toggleMovieFavorite(movie.id, userId)
                if (result.isSuccess) {
                    // Recargar la lista de favoritos
                    loadFavorites()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun addToFavorites(movie: Movie) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _uiState.value = _uiState.value.copy(errorMessage = "Debes iniciar sesión")
                    return@launch
                }
                
                // Toggle favorito en Firebase (lo agregará si no está marcado)
                val result = movieRepository.toggleMovieFavorite(movie.id, userId)
                if (result.isSuccess) {
                    // Recargar la lista de favoritos
                    loadFavorites()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    private fun getCurrentUserId(): String = authRepository.currentUser?.uid ?: "anonymous"
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun refreshFavorites() {
        loadFavorites()
    }
}