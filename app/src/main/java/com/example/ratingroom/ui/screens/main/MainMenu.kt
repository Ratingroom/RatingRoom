package com.example.ratingroom.ui.screens.main

import com.example.ratingroom.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.ui.utils.*
import com.example.ratingroom.data.models.Movie
import com.example.ratingroom.ui.theme.RatingRoomTheme

@Composable
fun MainMenuScreen(
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainMenuViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MainMenuScreenContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onGenreSelected = viewModel::onGenreSelected,
        onFilterExpandedChange = viewModel::onFilterExpandedChange,
        onMovieClick = onMovieClick,
        modifier = modifier
    )
}

@Composable
fun MainMenuScreenContent(
    uiState: MainMenuUIState,
    onSearchQueryChange: (String) -> Unit,
    onGenreSelected: (String) -> Unit,
    onFilterExpandedChange: (Boolean) -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    GradientBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Búsqueda
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = stringResource(id = R.string.main_menu_search_placeholder)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filtros (usar géneros del estado)
            FilterDropdown(
                expanded = uiState.filterExpanded,
                onExpandedChange = onFilterExpandedChange,
                selectedGenre = uiState.selectedGenre,
                genres = uiState.genres,      // ✅ viene del VM
                onGenreSelected = onGenreSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(id = R.string.main_menu_popular_movies),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = cs.tertiary
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null && uiState.filteredMovies.isEmpty() -> {
                    // Mensaje claro cuando no hay nada que mostrar
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Sin datos",
                            color = cs.onBackground
                        )
                    }
                }
                else -> {
                    if (uiState.filteredMovies.isEmpty()) {
                        // Caso raro: sin error pero vacío (señal visual)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay películas para mostrar", color = cs.onBackground)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            items(uiState.filteredMovies) { movie ->
                                MovieCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewMainMenuScreen() {
    RatingRoomTheme {
        MainMenuScreenContent(
            uiState = MainMenuUIState(
                isLoading = false,
                movies = listOf(
                    Movie(
                        id = 1,
                        title = "Titanic",
                        year = "1997",
                        genre = "Romance",
                        rating = 4.5,
                        reviews = 3876,
                        description = "desc",
                        director = "James Cameron",
                        duration = "3h14",
                        imageUrl = ""
                    )
                ),
                filteredMovies = listOf(
                    Movie(
                        id = 1,
                        title = "Titanic",
                        year = "1997",
                        genre = "Romance",
                        rating = 4.5,
                        reviews = 3876,
                        description = "desc",
                        director = "James Cameron",
                        duration = "3h14",
                        imageUrl = ""
                    )
                ),
                searchQuery = "",
                selectedGenre = "Todos",
                filterExpanded = false,
                genres = listOf("Todos", "Romance", "Acción"),
                errorMessage = null
            ),
            onSearchQueryChange = {},
            onGenreSelected = {},
            onFilterExpandedChange = {},
            onMovieClick = {}
        )
    }
}
