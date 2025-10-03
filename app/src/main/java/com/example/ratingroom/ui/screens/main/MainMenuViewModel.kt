package com.example.ratingroom.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.models.Movie
import com.example.ratingroom.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainMenuViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainMenuUIState(
            isLoading = true,
            movies = emptyList(),
            filteredMovies = emptyList(),
            genres = listOf("Todos"),
            searchQuery = "",
            selectedGenre = "Todos",
            filterExpanded = false,
            errorMessage = null
        )
    )
    val uiState: StateFlow<MainMenuUIState> = _uiState.asStateFlow()

    init {
        loadMovies()
    }

    /** Carga inicial (películas + géneros) */
    fun loadMovies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Cargar en paralelo
                val moviesDefer = async { MovieRepository.getAllMovies() }
                val genresDefer = async { MovieRepository.getGenres() }

                val movies = moviesDefer.await()
                val genres = genresDefer.await().ifEmpty { listOf("Todos") }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movies = movies,
                    filteredMovies = movies,   // 💡 mostrar todo al inicio
                    genres = genres,
                    errorMessage = if (movies.isEmpty()) "No hay películas disponibles" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error cargando películas"
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onGenreSelected(genre: String) {
        _uiState.value = _uiState.value.copy(
            selectedGenre = genre,
            filterExpanded = false
        )
        applyFilters()
    }

    fun onFilterExpandedChange(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(filterExpanded = expanded)
    }

    /** Aplica búsqueda + filtro por género sobre la lista ya cargada */
    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        val genre = state.selectedGenre

        val filtered = state.movies.filter { movie ->
            val matchesQuery = query.isEmpty() ||
                    movie.title.contains(query, ignoreCase = true) ||
                    movie.genre.contains(query, ignoreCase = true)

            val matchesGenre = genre == "Todos" || movie.genre.equals(genre, ignoreCase = true)
            matchesQuery && matchesGenre
        }

        _uiState.value = state.copy(
            filteredMovies = filtered,
            errorMessage = if (!state.isLoading && filtered.isEmpty()) "No hay resultados" else null
        )
    }

    /** Por si quieres recargar desde UI */
    fun refresh() = loadMovies()
}
