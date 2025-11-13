package com.example.ratingroom.ui.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.data.models.Movie
import com.example.ratingroom.repository.MovieRepository
import com.example.ratingroom.ui.theme.RatingRoomTheme
import com.example.ratingroom.ui.utils.MovieCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ListScreenContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onMovieClick = onMovieClick,
        onFavoriteClick = { movie -> viewModel.toggleMovieFavorite(movie.id) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreenContent(
    uiState: ListUIState,
    onTabSelected: (Int) -> Unit,
    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (Movie) -> Unit = {},
    modifier: Modifier = Modifier
) {
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1976D2),
                        Color(0xFF42A5F5)
                    )
                )
            )
    ) {
        // Header con estadísticas
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Stats(number = "12", label = "Por ver")
                Stats(number = "8", label = "Favoritos")
                Stats(number = "45", label = "Vistas")
            }
        }

        // Contenido principal con tabs
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Tab Row
                ListTabRow(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected
                )

                // Contenido de cada tab
                when (uiState.selectedTab) {
                    0 -> {
                        if (uiState.watchLaterMovies.isNotEmpty()) {
                            MovieList(
                                items = uiState.watchLaterMovies,
                                onMovieClick = onMovieClick,
                                onFavoriteClick = onFavoriteClick
                            )
                        } else {
                            EmptyTabContent(tabIndex = 0, modifier = Modifier.fillMaxSize())
                        }
                    }
                    1 -> {
                        if (uiState.favoriteMovies.isNotEmpty()) {
                            MovieList(
                                items = uiState.favoriteMovies,
                                onMovieClick = onMovieClick,
                                onFavoriteClick = onFavoriteClick
                            )
                        } else {
                            EmptyTabContent(tabIndex = 1, modifier = Modifier.fillMaxSize())
                        }
                    }
                    2 -> {
                        if (uiState.watchedMovies.isNotEmpty()) {
                            MovieList(
                                items = uiState.watchedMovies,
                                onMovieClick = onMovieClick,
                                onFavoriteClick = onFavoriteClick
                            )
                        } else {
                            EmptyTabContent(tabIndex = 2, modifier = Modifier.fillMaxSize())
                        }
                    }
                    else -> EmptyTabContent(tabIndex = 0, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun Stats(
    number: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ListTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Por ver", "Favoritos", "Vistas")

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = Color.Black,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = Color(0xFF2196F3)
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTab == index) Color(0xFF2196F3) else Color.Gray,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            )
        }
    }
}

@Composable
fun MovieList(
    items: List<Movie>,
    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (Movie) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(16.dp)
    ) {
        items(items) { movie ->
            MovieCard(
                movie = movie, 
                onClick = { onMovieClick(movie.id) },
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}@Composable
fun EmptyTabContent(tabIndex: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val message = when (tabIndex) {
        0 -> "Sin elementos por ver"
        1 -> "Aún no tienes favoritos"
        2 -> "Aún no has visto nada"
        else -> "Sin contenido"
    }

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VisibilityOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = cs.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = cs.onSurfaceVariant,
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewListScreen() {
    RatingRoomTheme {
        ListScreenContent(
            uiState = ListUIState(
                selectedTab = 0,
                watchLaterMovies = emptyList(),
                favoriteMovies = emptyList(),
                watchedMovies = emptyList()
            ),
            onTabSelected = {},
            onMovieClick = {}
        )
    }
}
