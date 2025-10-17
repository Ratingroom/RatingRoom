package com.example.ratingroom.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.repository.UserProfile
import com.example.ratingroom.ui.utils.AvatarInitials
import com.example.ratingroom.ui.utils.ErrorMessage
import androidx.compose.ui.tooling.preview.Preview
import com.example.ratingroom.ui.theme.RatingRoomTheme

@Composable
fun FollowersRoute(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadFollowers()
    }
    
    val followers by viewModel.followers.collectAsState()
    val isLoading by viewModel.isLoadingFollowers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    FollowersScreen(
        followers = followers,
        isLoading = isLoading,
        errorMessage = uiState.errorMessage,
        onBack = onBack,
        onUserClick = onUserClick,
        onFollowUser = viewModel::followUser,
        onUnfollowUser = viewModel::unfollowUser,
        onRetry = viewModel::loadFollowers
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersScreen(
    followers: List<UserProfile>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onFollowUser: (String) -> Unit,
    onUnfollowUser: (String) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguidores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                errorMessage != null -> {
                    ErrorMessage(
                        message = errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                followers.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Aún no tienes seguidores",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(followers) { follower ->
                            UserCard(
                                user = follower,
                                onUserClick = { onUserClick(follower.uid) },
                                onFollowClick = { onFollowUser(follower.uid) },
                                onUnfollowClick = { onUnfollowUser(follower.uid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FollowersScreenPreview() {
    RatingRoomTheme {
        FollowersScreen(
            followers = listOf(
                UserProfile(uid = "1", email = "ana@mail.com", fullName = "Ana Pérez"),
                UserProfile(uid = "2", email = "luis@mail.com", fullName = "Luis García", profileImageUrl = null)
            ),
            isLoading = false,
            errorMessage = null,
            onBack = {},
            onUserClick = {},
            onFollowUser = {},
            onUnfollowUser = {},
            onRetry = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserCard(
    user: UserProfile,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit
) {
    var isFollowing by remember { mutableStateOf(false) } // En una implementación real, esto vendría del backend
    
    Card(
        onClick = onUserClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarInitials(
                initials = user.fullName?.take(2)?.uppercase() ?: "??",
                imageUrl = user.profileImageUrl,
                size = 48.dp
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = user.fullName ?: "Usuario",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = {
                    if (isFollowing) onUnfollowClick() else onFollowClick()
                    isFollowing = !isFollowing
                }
            ) {
                Icon(
                    imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                    contentDescription = if (isFollowing) "Dejar de seguir" else "Seguir",
                    tint = if (isFollowing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}