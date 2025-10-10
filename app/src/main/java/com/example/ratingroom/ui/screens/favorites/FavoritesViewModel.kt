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
class FavoritesViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(FavoritesUIState())
    val uiState: StateFlow<FavoritesUIState> = _uiState.asStateFlow()
    
    init {
        loadFavorites()
    }
    
    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val favoriteMovies = MovieRepository.getFavoriteMovies()
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
                // TODO: Implementar lógica para remover de favoritos en el repositorio
                val updatedFavorites = _uiState.value.favoriteMovies.filter { it.id != movie.id }
                _uiState.value = _uiState.value.copy(
                    favoriteMovies = updatedFavorites
                )
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
                // TODO: Implementar lógica para agregar a favoritos en el repositorio
                val updatedFavorites = _uiState.value.favoriteMovies + movie
                _uiState.value = _uiState.value.copy(
                    favoriteMovies = updatedFavorites
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun refreshFavorites() {
        loadFavorites()
    }
}