package com.example.ratingroom.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.repository.UserProfile
import com.example.ratingroom.ui.utils.AvatarInitials
import com.example.ratingroom.ui.utils.ErrorMessage
import com.example.ratingroom.ui.theme.RatingRoomTheme

@Composable
fun FollowingRoute(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadFollowing()
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val following = uiState.following
    val isLoading = uiState.isLoadingFollowing
    
    FollowingScreen(
        following = following,
        isLoading = isLoading,
        errorMessage = uiState.errorMessage,
        onBack = onBack,
        onUserClick = onUserClick,
        onUnfollowUser = viewModel::unfollowUser,
        onRetry = viewModel::loadFollowing
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    following: List<UserProfile>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onUnfollowUser: (String) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Siguiendo") },
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
                
                following.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "No sigues a ningún usuario",
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
                        items(following) { followedUser ->
                            FollowingUserCard(
                                user = followedUser,
                                onUserClick = { onUserClick(followedUser.uid) },
                                onUnfollowClick = { onUnfollowUser(followedUser.uid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowingUserCard(
    user: UserProfile,
    onUserClick: () -> Unit,
    onUnfollowClick: () -> Unit
) {
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
            
            IconButton(onClick = onUnfollowClick) {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = "Dejar de seguir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FollowingScreenPreview() {
    RatingRoomTheme {
        FollowingScreen(
            following = listOf(
                UserProfile(uid = "1", email = "maria@mail.com", fullName = "María López"),
                UserProfile(uid = "3", email = "carlos@mail.com", fullName = "Carlos Díaz")
            ),
            isLoading = false,
            errorMessage = null,
            onBack = {},
            onUserClick = {},
            onUnfollowUser = {},
            onRetry = {}
        )
    }
}