package com.example.ratingroom.ui.screens.moviedetail

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.ratingroom.data.models.Review
import com.example.ratingroom.ui.utils.MoviePoster

@Composable
fun MovieDetailRoute(
    movieId: Int,
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(movieId) { viewModel.loadMovieDetail(movieId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MovieDetailScreen(
        uiState = uiState,
        movieId = movieId,
        currentUserId = viewModel.getCurrentUserId(),
        onBack = onBack,
        onClearError = { viewModel.clearError() },
        onCreateReview = { rating, texto -> viewModel.createReview(movieId, rating, texto) },
        onLikeClick = { reviewId, userId -> viewModel.sendOrDeleteLike(reviewId, userId) },
        followingOnly = uiState.showFollowingOnly,
        onToggleFollowingOnly = { viewModel.setShowFollowingOnly(it) }
    )
}

@Composable
fun MovieDetailScreen(
    uiState: MovieDetailUIState,
    movieId: Int,
    currentUserId: String,
    onBack: () -> Unit = {},
    onClearError: () -> Unit = {},
    onCreateReview: (Int, String) -> Unit = { _, _ -> },
    onLikeClick: (String, String) -> Unit = { _, _ -> },
    followingOnly: Boolean,                               // NUEVO
    onToggleFollowingOnly: (Boolean) -> Unit              // NUEVO
) {
    val movie = uiState.movie
    val snackbarHostState = SnackbarHostState()
    var showCreateReview by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            // TopBar estable
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = movie?.title ?: "Detalle",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        floatingActionButton = {
            if (movie != null) {
                FloatingActionButton(
                    onClick = { showCreateReview = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Crear reseña"
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    action = { TextButton(onClick = onClearError) { Text("OK") } }
                ) { Text(data.visuals.message) }
            }
        }
    ) { inner ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            movie != null -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        // Poster 2:3
                        MoviePoster(
                            imageUrl = movie.imageUrl,
                            movieTitle = movie.title,
                            width = 260.dp,
                            height = 390.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                        )
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "${movie.year} • ${movie.duration}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = movie.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(16.dp))

                        // Encabezado de reseñas + Toggle "Solo seguidos"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reseñas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Solo seguidos",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = followingOnly,                        // CAMBIO
                                    onCheckedChange = { onToggleFollowingOnly(it) }, // CAMBIO
                                    enabled = currentUserId.isNotBlank()
                                )
                            }
                        }

                        // Hint si no autenticado
                        if (currentUserId.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Inicia sesión para filtrar por usuarios que sigues.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    items(uiState.reviews) { review ->
                        ReviewItem(
                            review = review,
                            onLikeClick = {
                                onLikeClick(review.id, currentUserId)
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }

            else -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontró la película",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Diálogo para crear reseña
    if (showCreateReview) {
        ReviewEditorDialog(
            title = "Nueva reseña",
            initialRating = 5,
            initialText = "",
            onDismiss = { showCreateReview = false },
            onConfirm = { rating, text ->
                onCreateReview(rating, text)
                showCreateReview = false
            }
        )
    }
}

@Composable
private fun ReviewItem(
    review: Review,
    onLikeClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            // Header con información del usuario
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar con imagen real
                if (!review.userImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = review.userImageUrl,
                        contentDescription = "Imagen de perfil de ${review.userName}",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = review.userName?.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName ?: "Usuario",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = review.date,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "⭐ ${review.rating}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = review.comment,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Botón de Like
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (review.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (review.isLiked) "Quitar like" else "Dar like",
                        tint = if (review.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (review.likes > 0) {
                    Text(
                        text = review.likes.toString(),
                        color = if (review.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewEditorDialog(
    title: String,
    initialRating: Int,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var ratingText by remember { mutableStateOf(initialRating.toString()) }
    var comment by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ratingText,
                    onValueChange = { if (it.all(Char::isDigit)) ratingText = it },
                    label = { Text("Rating (1-5)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comentario") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val rating = ratingText.toIntOrNull()?.coerceIn(1, 5) ?: 5
                onConfirm(rating, comment.trim())
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
