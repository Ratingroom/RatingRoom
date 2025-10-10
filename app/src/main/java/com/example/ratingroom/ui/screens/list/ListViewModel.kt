package com.example.ratingroom.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ListViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(ListUIState())
    val uiState: StateFlow<ListUIState> = _uiState.asStateFlow()
    
    init {
        loadLists()
    }
    
    private fun loadLists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val watchLaterMovies = MovieRepository.getWatchLaterMovies()
                val favoriteMovies = MovieRepository.getFavoriteMovies()
                val watchedMovies = MovieRepository.getWatchedMovies()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    watchLaterMovies = watchLaterMovies,
                    favoriteMovies = favoriteMovies,
                    watchedMovies = watchedMovies
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun onTabSelected(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}