package com.example.ratingroom.ui.screens.profile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ratingroom.data.remote.ReviewDto
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

    // Refrescar/cargar perfil
    LaunchedEffect(Unit) { viewModel.loadProfile() }

    val handleLogout = {
        viewModel.logout()
        onLogoutClick()
    }

    // Callbacks CRUD
    val onCreateReview: (Int, String) -> Unit = { rating, text ->
        viewModel.createReview(articuloId = 77, rating = rating, texto = text)
    }
    val onEditReview: (Int, Int, String) -> Unit = { reviewId, rating, text ->
        viewModel.updateReview(reviewId, rating, text)
    }
    val onDeleteReview: (Int) -> Unit = { reviewId ->
        viewModel.deleteReview(reviewId)
    }

    ProfileScreenContent(
        uiState = uiState,
        isDarkMode = isDarkMode,
        onEditClick = onEditClick,
        onLogoutClick = handleLogout,
        onDarkModeChange = viewModel::onDarkModeChange,
        onCreateReview = onCreateReview,
        onEditReview = onEditReview,
        onDeleteReview = onDeleteReview,
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
    onCreateReview: (Int, String) -> Unit,
    onEditReview: (Int, Int, String) -> Unit,
    onDeleteReview: (Int) -> Unit,
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

                    // Lista CRUD de reseñas
                    ReviewsSection(
                        reviews = uiState.reviews,
                        colorScheme = cs,
                        onCreate = onCreateReview,
                        onEdit = onEditReview,
                        onDelete = onDeleteReview
                    )

                    Spacer(Modifier.height(12.dp))

                    // Lista de reseñas recientes (otra rama)
                    RecentReviews(
                        colorScheme = cs,
                        reviews = uiState.userReviews
                    )

                    Spacer(Modifier.height(12.dp))

                    uiState.errorMessage?.let { msg ->
                        Text("Error: $msg", color = cs.error)
                    }
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
            val imageUrl = profileData.profileImageUrl
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
                text = profileData.email ?: "",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            InfoRow(icon = Icons.Filled.CalendarMonth, text = "Miembro desde ${profileData.memberSince ?: "—"}")
            Spacer(Modifier.height(8.dp))
            InfoChip(text = "Género favorito: ${profileData.favoriteGenre ?: "—"}", icon = Icons.Filled.Category)
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
                }
            }
        )
    }
}

/* -------------------- CRUD Reseñas -------------------- */

@Composable
private fun ReviewsSection(
    reviews: List<ReviewDto>,
    colorScheme: ColorScheme,
    onCreate: (Int, String) -> Unit,
    onEdit: (Int, Int, String) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }

    SectionCard(title = "Mis Reseñas") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total: ${reviews.size}", color = colorScheme.onSurface)
            TextButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Nueva reseña")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (reviews.isEmpty()) {
            Text("Aún no tienes reseñas.", color = colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reviews.forEach { r ->
                    ReviewRow(
                        review = r,
                        onEdit = { rating, text -> onEdit(r.id, rating, text) },
                        onDelete = { onDelete(r.id) }
                    )
                }
            }
        }
    }

    if (showCreate) {
        ReviewEditorDialog(
            title = "Nueva reseña",
            initialRating = 5,
            initialText = "",
            onDismiss = { showCreate = false },
            onConfirm = { rating, text ->
                onCreate(rating, text)
                showCreate = false
            }
        )
    }
}

@Composable
private fun ReviewRow(
    review: ReviewDto,
    onEdit: (Int, String) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("⭐ ${review.rating}/5", style = MaterialTheme.typography.titleSmall)
            Text(review.texto, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editing = true }) { Text("Editar") }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            }
        }
    }

    if (editing) {
        ReviewEditorDialog(
            title = "Editar reseña",
            initialRating = review.rating,
            initialText = review.texto,
            onDismiss = { editing = false },
            onConfirm = { rating, text ->
                onEdit(rating, text)
                editing = false
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
                reviews = listOf(
                    ReviewDto(id = 1, usuario_id = 1, pelicula_id = 77, rating = 5, texto = "Excelente!"),
                    ReviewDto(id = 2, usuario_id = 1, pelicula_id = 77, rating = 4, texto = "Muy buena.")
                ),
                userReviews = listOf(
                    Review(id = 1, movieId = 77, rating = 5.0, comment = "Muy buena", date = "Hace 2 días")
                ),
                isDarkMode = false,
                isLoading = false
            ),
            isDarkMode = false,
            onEditClick = {},
            onLogoutClick = {},
            onDarkModeChange = {},
            onCreateReview = { _, _ -> },
            onEditReview = { _, _, _ -> },
            onDeleteReview = {}
        )
    }
}
