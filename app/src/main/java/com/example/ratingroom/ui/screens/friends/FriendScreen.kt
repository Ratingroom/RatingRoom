@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ratingroom.ui.screens.friends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.ui.utils.AvatarInitials
import com.example.ratingroom.ui.utils.GradientBackground

@Composable
fun FriendRoute(
    userId: String,
    onBack: () -> Unit,
    viewModel: FriendViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) { viewModel.load(userId) }
    val uiState by viewModel.uiState.collectAsState()
    FriendScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onBack = onBack
    )
}

@Composable
fun FriendScreen(
    uiState: FriendUIState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Perfil") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás"
                            )
                        }
                    }
                )
            }
        ) { inner ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }

                    uiState.errorMessage != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = onRetry) { Text("Reintentar") }
                        }
                    }

                    uiState.profile != null -> {
                        FriendProfileWithReviews(
                            profile = uiState.profile,
                            isLoadingReviews = uiState.isLoadingReviews,
                            reviews = uiState.reviews
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendProfileWithReviews(
    profile: FriendProfileData,
    isLoadingReviews: Boolean,
    reviews: List<ReviewDto>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Cabecera del perfil
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarInitials(
                    initials = profile.name.take(2).uppercase(),
                    imageUrl = profile.profileImageUrl
                )
                Spacer(Modifier.height(12.dp))
                Text(profile.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                ProfileField(label = "Miembro desde", value = profile.memberSince ?: "—")
                ProfileField(label = "Género favorito", value = profile.favoriteGenre ?: "—")
                ProfileField(label = "Ubicación", value = profile.location ?: "—")
                ProfileField(label = "Biografía", value = profile.biography ?: "—")
                ProfileField(label = "Sitio web", value = profile.website ?: "—")
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(4.dp))
                Text("Reseñas", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Loading reseñas
        if (isLoadingReviews) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Lista de reseñas (si hay)
        if (!isLoadingReviews && reviews.isEmpty()) {
            item {
                Text(
                    "Este usuario aún no tiene reseñas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(reviews, key = { it.id }) { r ->
                ReviewCard(r)
            }
        }
    }
}

@Composable
private fun ReviewCard(r: ReviewDto) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text = "⭐ ${r.rating}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(text = r.texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
