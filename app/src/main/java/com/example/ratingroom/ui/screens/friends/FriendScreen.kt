@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ratingroom.ui.screens.friends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.ui.screens.profile.ProfileData
import com.example.ratingroom.ui.utils.AvatarInitials
import com.example.ratingroom.ui.utils.GradientBackground

@Composable
fun FriendRoute(
    userId: String,
    onBack: () -> Unit,
    viewModel: FriendViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) { viewModel.load(userId) }
    val ui by viewModel.ui.collectAsState()

    FriendScreen(
        ui = ui,
        onFollowToggle = viewModel::toggleFollow,
        onOpenFollowers = viewModel::openFollowers,
        onOpenFollowing = viewModel::openFollowing,
        onCloseDialogs = viewModel::closeDialogs,
        onBack = onBack
    )
}

@Composable
fun FriendScreen(
    ui: FriendUIState,
    onFollowToggle: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onCloseDialogs: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(ui.profile?.name ?: "Perfil") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
                    ui.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.errorMessage != null -> Text(
                        ui.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    ui.profile != null -> FriendContent(
                        profile = ui.profile,
                        isFollowing = ui.isFollowing,
                        followersCount = ui.followersCount,
                        followingCount = ui.followingCount,
                        isLoadingReviews = ui.isLoadingReviews,
                        reviews = ui.reviews,
                        onFollowToggle = onFollowToggle,
                        onOpenFollowers = onOpenFollowers,
                        onOpenFollowing = onOpenFollowing
                    )
                }
            }
        }
    }

    if (ui.showFollowers) NamesDialog("Seguidores", ui.followersNames, onCloseDialogs)
    if (ui.showFollowing) NamesDialog("Siguiendo", ui.followingNames, onCloseDialogs)
}

@Composable
private fun FriendContent(
    profile: ProfileData,
    isFollowing: Boolean,
    followersCount: Int,
    followingCount: Int,
    isLoadingReviews: Boolean,
    reviews: List<ReviewDto>,
    onFollowToggle: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Cabecera del perfil
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarInitials(
                        initials = profile.name.take(2).uppercase(),
                        imageUrl = profile.profileImageUrl,
                        size = 72.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    profile.email?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoRow(Icons.Filled.CalendarMonth, "Miembro desde ${profile.memberSince ?: "—"}")
                    Spacer(Modifier.height(4.dp))
                    InfoRow(Icons.Filled.Category, "Género favorito: ${profile.favoriteGenre ?: "—"}")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onFollowToggle) {
                        if (isFollowing) {
                            Icon(Icons.Filled.PersonRemove, contentDescription = null)
                            Spacer(Modifier.width(8.dp)); Text("Dejar de seguir")
                        } else {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp)); Text("Seguir")
                        }
                    }
                }
            }
        }

        // Métricas
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    label = "Seguidores",
                    value = followersCount.toString(),
                    onClick = onOpenFollowers,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Siguiendo",
                    value = followingCount.toString(),
                    onClick = onOpenFollowing,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Reseñas
        item {
            Text(
                "Reseñas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (isLoadingReviews) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
        } else if (reviews.isEmpty()) {
            item {
                Text(
                    "Este usuario aún no tiene reseñas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(reviews, key = { it.id }) { r ->
                ReviewCard(r)
            }
        }
    }
}

/* ---- helpers ---- */

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NamesDialog(title: String, items: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (items.isEmpty()) Text("Sin resultados")
            else LazyColumn { items(items) { Text(it) } }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun ReviewCard(r: ReviewDto) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("⭐ ${r.rating}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(r.texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
