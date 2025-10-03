package com.example.ratingroom.ui.screens.profile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.data.models.Review
import com.example.ratingroom.ui.theme.RatingRoomTheme
import com.example.ratingroom.ui.utils.*

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode = uiState.isDarkMode.takeIf { uiState.profileData != null } ?: isSystemInDarkTheme()
    
    // Refrescar el perfil cuando se regresa a la pantalla
    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }
    
    // Función para manejar el cierre de sesión
    val handleLogout = {
        viewModel.logout()
        onLogoutClick()
    }

    ProfileScreenContent(
        uiState = uiState,
        isDarkMode = isDarkMode,
        onEditClick = onEditClick,
        onLogoutClick = handleLogout,
        onDarkModeChange = viewModel::onDarkModeChange,
        modifier = modifier
    )
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUIState,
    isDarkMode: Boolean,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    GradientBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            Surface(
                color = cs.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    } else {
                        uiState.profileData?.let { profileData ->
                            ProfileHeader(profileData = profileData, colorScheme = cs)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    uiState.profileData?.let { pd ->
                        ProfileMetrics(
                            reviewsCount = pd.reviewsCount,
                            averageRating = pd.averageRating,
                            colorScheme = cs
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    ProfileSettings(
                        isDarkMode = isDarkMode,
                        onDarkModeChange = onDarkModeChange,
                        colorScheme = cs,
                        onEditClick = onEditClick,
                        onLogoutClick = onLogoutClick
                    )

                    Spacer(Modifier.height(12.dp))

                    RecentReviews(
                        colorScheme = cs,
                        reviews = uiState.userReviews
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    profileData: ProfileData,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    SectionCard(title = "") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            println("ProfileHeader: Mostrando perfil con nombre: ${profileData.name}, imageUrl: ${profileData.profileImageUrl}")
            val imageUrl = profileData.profileImageUrl
            println("ProfileHeader: URL de imagen a mostrar: $imageUrl")
            AvatarInitials(
                initials = profileData.name.take(2).uppercase(),
                imageUrl = imageUrl
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = profileData.name,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colorScheme.onSurface
            )
            Text(
                text = profileData.email,
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            InfoRow(icon = Icons.Filled.CalendarMonth, text = "Miembro desde ${profileData.memberSince}")
            Spacer(Modifier.height(8.dp))
            InfoChip(text = "Género favorito: ${profileData.favoriteGenre}", icon = Icons.Filled.Category)
        }
    }
}

@Composable
fun ProfileMetrics(
    reviewsCount: Int,
    averageRating: Double,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(
            icon = Icons.Filled.ChatBubbleOutline,
            number = reviewsCount.toString(),
            label = "Reseñas",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            icon = Icons.Filled.Star,
            number = String.format("%.1f", averageRating),
            label = "Promedio",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ProfileSettings(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    colorScheme: ColorScheme,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(title = "Configuración") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = "Modo oscuro",
                    tint = colorScheme.onSurface
                )
                Spacer(Modifier.width(12.dp))
                Text(text = "Modo oscuro", color = colorScheme.onSurface, fontSize = 16.sp)
            }
            Switch(checked = isDarkMode, onCheckedChange = onDarkModeChange)
        }

        HorizontalDivider()

        SettingsList(
            items = listOf(
                SettingsItem("edit", "Editar perfil", Icons.Filled.Person),
                SettingsItem("privacy", "Configuración de privacidad", Icons.Filled.Lock),
                SettingsItem("notif", "Notificaciones", Icons.Filled.Notifications),
                SettingsItem("logout", "Cerrar sesión", Icons.Filled.Logout, tint = colorScheme.error)
            ),
            onItemClick = { id ->
                when (id) {
                    "edit" -> onEditClick()
                    "logout" -> onLogoutClick()
                    "privacy", "notif" -> { /* sin acción por ahora */ }
                }
            }
        )
    }
}

@Composable
fun RecentReviews(
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
    reviews: List<Review> = emptyList()
) {
    SectionCard(title = "Mis Reseñas Recientes") {
        if (reviews.isEmpty()) {
            Text(
                text = "No has realizado ninguna reseña todavía",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            reviews.forEachIndexed { index, review ->
                ReviewCard(
                    title = "Película #${review.movieId}", 
                    rating = review.rating.toInt(),
                    excerpt = review.comment,
                    timeAgo = review.date
                )
                if (index < reviews.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    RatingRoomTheme {
        ProfileScreenContent(
            uiState = ProfileUIState(
                profileData = ProfileData(
                    name = "Pedro",
                    email = "pedro@correo.com",
                    memberSince = "Enero 2024",
                    favoriteGenre = "Sci-Fi",
                    reviewsCount = 3,
                    averageRating = 4.7,
                    profileImageUrl = null
                ),
                isDarkMode = false,
                isLoading = false
            ),
            isDarkMode = false,
            onEditClick = {},
            onLogoutClick = {},
            onDarkModeChange = {}
        )
    }
}

