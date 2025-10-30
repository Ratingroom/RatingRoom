package com.example.ratingroom.ui.screens.main

import androidx.paging.PagingData
import com.example.ratingroom.data.models.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class MainMenuUIState(
    val isLoading: Boolean = true,
    val movies: List<Movie> = emptyList(),
    val filteredMovies: List<Movie> = emptyList(),
    val searchQuery: String = "",
    val selectedGenre: String = "Todos",
    val filterExpanded: Boolean = false,
    val genres: List<String> = listOf("Todos"),
    val errorMessage: String? = null,
    // Paginación
    val usePagination: Boolean = true,
    val currentPage: Int = 0,
    val hasMorePages: Boolean = true,
    val pageSize: Int = 10  // 🎯 Cambiado de 6 a 10 películas por página
)


