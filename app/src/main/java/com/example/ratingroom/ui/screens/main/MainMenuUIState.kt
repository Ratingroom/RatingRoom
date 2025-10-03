package com.example.ratingroom.ui.screens.main
import com.example.ratingroom.data.models.Movie

data class MainMenuUIState(
    val isLoading: Boolean = true,
    val movies: List<Movie> = emptyList(),
    val filteredMovies: List<Movie> = emptyList(),
    val searchQuery: String = "",
    val selectedGenre: String = "Todos",
    val filterExpanded: Boolean = false,
    val genres: List<String> = listOf("Todos"),
    val errorMessage: String? = null
)

