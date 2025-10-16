package com.example.ratingroom.ui.screens.synopsis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.repository.MovieRepository
import com.example.ratingroom.ui.screens.synopsis.CastMember
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SynopsisViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SynopsisUIState())
    val uiState: StateFlow<SynopsisUIState> = _uiState.asStateFlow()
    
    fun loadMovieSynopsis(movieId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val movie = movieRepository.getMovieById(movieId)
                val cast = listOf(
                    CastMember("Actor Principal", "Protagonista", true),
                    CastMember("Actriz Principal", "Protagonista", true),
                    CastMember("Actor Secundario 1", "Secundario"),
                    CastMember("Actriz Secundaria 2", "Secundaria")
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movie = movie,
                    cast = cast
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
}