package com.example.ratingroom.ui.screens.main

import com.example.ratingroom.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage,
        onFavoriteClick = { movie -> viewModel.toggleMovieFavorite(movie.id) },
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
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onFavoriteClick: (Movie) -> Unit = {},
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(id = R.string.main_menu_popular_movies),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.tertiary
                )
                
                // Indicador de página
                if (uiState.usePagination) {
                    Text(
                        text = "Página ${uiState.currentPage + 1}",
                        fontSize = 14.sp,
                        color = cs.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

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
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Grid de películas con estado de scroll
                            val gridState = rememberLazyGridState()
                            
                            // Detectar si el usuario ha hecho scroll
                            val isScrolled by remember {
                                derivedStateOf {
                                    gridState.firstVisibleItemIndex > 0 || 
                                    gridState.firstVisibleItemScrollOffset > 0
                                }
                            }
                            
                            // Detectar si está cerca del final (últimos 2 items visibles)
                            val isNearEnd by remember {
                                derivedStateOf {
                                    val layoutInfo = gridState.layoutInfo
                                    val totalItems = layoutInfo.totalItemsCount
                                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    totalItems > 0 && lastVisibleItem >= totalItems - 2  // 🎯 Cambiado de 3 a 5
                                }
                            }
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(uiState.filteredMovies) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = { onMovieClick(movie.id) },
                                        onFavoriteClick = onFavoriteClick
                                    )
                                }
                            }
                            
                            // Controles de paginación - Solo mostrar cuando el usuario ha hecho scroll
                            // y está cerca del final O hay más de una página disponible
                            if (uiState.usePagination && (isNearEnd || uiState.currentPage > 0)) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                PaginationControls(
                                    currentPage = uiState.currentPage,
                                    hasMorePages = uiState.hasMorePages,
                                    onPreviousPage = onPreviousPage,
                                    onNextPage = onNextPage,
                                    isLoading = uiState.isLoading
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
            onMovieClick = {},
            onNextPage = {},
            onPreviousPage = {}
        )
    }
}

@Composable
fun PaginationControls(
    currentPage: Int,
    hasMorePages: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón Anterior
        Button(
            onClick = onPreviousPage,
            enabled = currentPage > 0 && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                disabledContainerColor = cs.surfaceVariant
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Página anterior",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Anterior")
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Botón Siguiente
        Button(
            onClick = onNextPage,
            enabled = hasMorePages && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                disabledContainerColor = cs.surfaceVariant
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Siguiente")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Página siguiente",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
