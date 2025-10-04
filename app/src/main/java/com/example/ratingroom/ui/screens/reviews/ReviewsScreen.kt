package com.example.ratingroom.ui.screens.reviews

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.ui.utils.AppTopBar
import com.example.ratingroom.ui.utils.TopBarConfig
import com.example.ratingroom.ui.utils.EmptyActivityState
import com.example.ratingroom.ui.utils.ReviewCard
import com.example.ratingroom.ui.utils.ReviewEditorDialog
import com.example.ratingroom.ui.theme.RatingRoomTheme

@Composable
fun ReviewsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ReviewsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onEditReview = { reviewId, rating, texto -> viewModel.editReview(reviewId, rating, texto) },
        onDeleteReview = { reviewId -> viewModel.deleteReview(reviewId) },
        onClearError = viewModel::clearError,
        modifier = modifier
    )
}

@Composable
fun ReviewsScreenContent(
    uiState: ReviewsUIState,
    onBack: () -> Unit,
    onEditReview: (Int, Int, String) -> Unit,
    onDeleteReview: (Int) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingReview by remember { mutableStateOf<ReviewItem?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                TopBarConfig(
                    title = "Mis Reseñas",
                    showBackButton = true,
                    onBackClick = onBack
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(Modifier.padding(padding).padding(16.dp)) {
                    repeat(3) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .padding(bottom = 12.dp),
                            tonalElevation = 1.dp
                        ) {}
                    }
                }
            }
            uiState.reviews.isEmpty() && !uiState.isLoading -> {
                EmptyActivityState(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.reviews) { review ->
                        ReviewCard(
                            title = review.movieTitle,
                            rating = review.rating,
                            excerpt = review.comment,
                            timeAgo = "Hace 3 días",
                            onEdit = { editingReview = review },
                            onDelete = { onDeleteReview(review.id) }
                        )
                    }
                }
            }
        }
    }

    // Dialog para editar reseña
    editingReview?.let { review ->
        ReviewEditorDialog(
            title = "Editar reseña",
            initialRating = review.rating,
            initialText = review.comment,
            onDismiss = { editingReview = null },
            onConfirm = { rating, text ->
                onEditReview(review.id, rating, text)
                editingReview = null
            }
        )
    }

    // Mostrar errores
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Aquí podrías mostrar un Snackbar si quisieras
            onClearError()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ReviewsScreenPreview() {
    RatingRoomTheme {
        ReviewsScreenContent(
            uiState = ReviewsUIState(
                reviews = listOf(
                    ReviewItem(
                        id = 1,
                        movieId = 1,
                        movieTitle = "Inception",
                        rating = 5,
                        comment = "Una película increíble que te hace pensar."
                    ),
                    ReviewItem(
                        id = 2,
                        movieId = 2,
                        movieTitle = "The Matrix",
                        rating = 4,
                        comment = "Un clásico del cine de ciencia ficción."
                    )
                ),
                isLoading = false
            ),
            onBack = {},
            onEditReview = { _, _, _ -> },
            onDeleteReview = { },
            onClearError = {}
        )
    }
}

