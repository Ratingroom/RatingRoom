package com.example.ratingroom.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ratingroom.ui.utils.*
import com.example.ratingroom.ui.theme.RatingRoomTheme
import com.example.ratingroom.repository.FriendsRepository
import com.example.ratingroom.data.models.Friend
import com.example.ratingroom.data.models.FriendActivity
import com.example.ratingroom.data.models.FriendshipType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit = {},
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    FriendsScreenContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onTabSelected = viewModel::onTabSelected,
        onFriendAction = { friend, action -> viewModel.onFriendAction(friend, action) },
        onUserClick = onUserClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreenContent(
    uiState: FriendsUIState,
    onSearchQueryChange: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onFriendAction: (Friend, String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header con estadísticas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(16.dp)
        ) {
            val followingCount = uiState.friends.size
            val followersCount = uiState.followers.size
            val mutualCount = uiState.friends.count { f ->
                val fu = f.uid
                if (!fu.isNullOrBlank()) uiState.followers.any { it.uid == fu }
                else uiState.followers.any { it.id == f.id }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(number = followingCount.toString(), label = "Siguiendo", modifier = Modifier.weight(1f))
                StatItem(number = followersCount.toString(), label = "Seguidores", modifier = Modifier.weight(1f))
                StatItem(number = mutualCount.toString(), label = "Mutuos", modifier = Modifier.weight(1f))
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Buscar amigos...",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                FriendsTabRow(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (uiState.selectedTab) {
                    0 -> ActivityTab(
                        searchQuery = uiState.searchQuery,
                        onFriendAction = onFriendAction,
                        onUserClick = onUserClick,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> {
                        val currentList = when (uiState.selectedTab) {
                            1 -> uiState.friends
                            2 -> uiState.followers
                            3 -> uiState.suggestions
                            else -> emptyList()
                        }
                        FriendsListTab(
                            tabIndex = uiState.selectedTab,
                            searchQuery = uiState.searchQuery,
                            friends = currentList,
                            onFriendAction = onFriendAction,
                            onUserClick = onUserClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(number: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun FriendsTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Actividad", "Siguiendo", "Seguidores", "Descubrir")

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = MaterialTheme.colorScheme.primary
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
                        color = if (selectedTab == index)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            )
        }
    }
}

@Composable
fun ActivityTab(
    searchQuery: String,
    onFriendAction: (Friend, String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val friendsActivity = remember { FriendsRepository.getFriendsActivity() }

    val filteredActivity = remember(searchQuery) {
        if (searchQuery.isBlank()) friendsActivity
        else friendsActivity.filter {
            it.friend.name.contains(searchQuery, ignoreCase = true) ||
                    it.movie.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        if (filteredActivity.isEmpty()) {
            item {
                EmptyActivityState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        } else {
            items(filteredActivity) { activity ->
                Box(modifier = Modifier.clickable {
                    onUserClick(activity.friend.uid ?: activity.friend.id.toString())
                }) {
                    FriendActivityCard(
                        activity = activity,
                        onAction = { action -> onFriendAction(activity.friend, action) }
                    )
                }
            }
        }
    }
}

@Composable
fun FriendsListTab(
    tabIndex: Int,
    searchQuery: String,
    friends: List<Friend>,
    onFriendAction: (Friend, String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val friendsList = remember(friends, searchQuery) {
        if (searchQuery.isBlank()) friends
        else friends.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        if (friendsList.isEmpty()) {
            item {
                EmptyFriendsState(
                    tabIndex = tabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        } else {
            items(friendsList) { friend ->
                FriendCard(
                    friend = friend,
                    tabIndex = tabIndex,
                    onAction = { action -> onFriendAction(friend, action) },
                    onUserClick = onUserClick
                )
            }
        }
    }
}

@Composable
fun FriendCard(
    friend: Friend,
    tabIndex: Int,
    onAction: (String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUserClick(friend.uid ?: friend.id.toString()) }, // ✅ cambio clave
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = cs.surfaceVariant,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = friend.name.first().toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = friend.username,
                    fontSize = 14.sp,
                    color = cs.onSurfaceVariant
                )
            }

            // Botón seguir / mensaje / perfil
            IconButton(onClick = { onUserClick(friend.uid ?: friend.id.toString()) }) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Ver perfil",
                    tint = cs.onSurfaceVariant
                )
            }
        }
    }
}
