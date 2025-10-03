package com.example.ratingroom.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.models.Review
import com.example.ratingroom.data.repository.AuthRepository
import com.example.ratingroom.Config.CURRENT_USER_ID
import com.example.ratingroom.data.remote.UserProfileDto
import com.example.ratingroom.data.remote.RetrofitClient
import com.example.ratingroom.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@HiltViewModel
class ProfileViewModel @Inject constructor(
) : ViewModel() {

    private val repo = ReviewRepository(RetrofitClient.reviewApi)

    private val _uiState = MutableStateFlow(ProfileUIState(isLoading = true))
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    // ID del usuario actual (quemado como en los requisitos)
    private val currentUserId = 1

    init {
        loadProfile()
    }

    fun loadProfile(userId: Int = CURRENT_USER_ID) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repo.getUserProfile(userId) }
                .onSuccess { prof ->
                    val reviews = prof?.reviews ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profileData = prof?.toProfileData(),
                        reviews = reviews
                    )
                    println("ProfileViewModel.loadProfile: Estado actualizado con profileImageUrl: ${_uiState.value.profileData?.profileImageUrl}")
                    
                    // Cargar las reseñas del usuario después de cargar el perfil
                    loadUserReviews()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar perfil"
                    )
                }
        }
    }
    
    private fun loadUserReviews() {
        viewModelScope.launch {
            try {
                // Simulamos una llamada al backend para obtener las reseñas del usuario actual
                delay(500) // Simular tiempo de respuesta de la API
                
                // Reseñas del usuario actual (simuladas)
                val userReviews = listOf(
                    Review(
                        id = 1,
                        movieId = 101,
                        userId = currentUserId,
                        rating = 5.0,
                        comment = "Una película increíble que te hace pensar. Los efectos visuales son espectaculares y la historia es muy original.",
                        date = "Hace 3 días"
                    ),
                    Review(
                        id = 2,
                        movieId = 102,
                        userId = currentUserId,
                        rating = 4.0,
                        comment = "Clásico de la ciencia ficción. Revolucionó el género y sigue vigente.",
                        date = "Hace 1 semana"
                    ),
                    Review(
                        id = 3,
                        movieId = 103,
                        userId = currentUserId,
                        rating = 5.0,
                        comment = "Obra maestra: ciencia, emoción e imágenes se combinan perfectamente.",
                        date = "Hace 2 semanas"
                    )
                )
                
                // Actualizar el estado con las reseñas del usuario
                _uiState.value = _uiState.value.copy(
                    userReviews = userReviews,
                    reviewsCount = userReviews.size,
                    averageRating = userReviews.map { it.rating }.average()
                )
                
                // Actualizar también el profileData con los nuevos conteos
                _uiState.value.profileData?.let { profileData ->
                    val updatedProfileData = profileData.copy(
                        reviewsCount = userReviews.size,
                        averageRating = userReviews.map { it.rating }.average()
                    )
                    _uiState.value = _uiState.value.copy(profileData = updatedProfileData)
                }
                
            } catch (e: Exception) {
                println("ProfileViewModel.loadUserReviews: ERROR al cargar reseñas: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error al cargar reseñas"
                )
            }
        }
    }

    fun createReview(articuloId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            runCatching { repo.create(CURRENT_USER_ID, articuloId, rating, texto) }
                .onSuccess { created ->
                    if (created != null) {
                        _uiState.value = _uiState.value.copy(
                            reviews = listOf(created) + _uiState.value.reviews
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
        }
    }

    fun updateReview(reviewId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            runCatching { repo.update(CURRENT_USER_ID, reviewId, rating, texto) }
                .onSuccess { updated ->
                    if (updated != null) {
                        _uiState.value = _uiState.value.copy(
                            reviews = _uiState.value.reviews.map { if (it.id == reviewId) updated else it }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            runCatching { repo.delete(CURRENT_USER_ID, reviewId) }
                .onSuccess { ok ->
                    if (ok) {
                        _uiState.value = _uiState.value.copy(
                            reviews = _uiState.value.reviews.filterNot { it.id == reviewId }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
        }
    }

    fun onDarkModeChange(isDarkMode: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = isDarkMode)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Mapper del DTO del backend a datos de UI
    private fun UserProfileDto.toProfileData() = ProfileData(
        name = nombre ?: usuario,
        email = email,
        memberSince = null,
        favoriteGenre = null,
        reviewsCount = reviews.size,
        averageRating = reviews.map { it.rating }.average().takeIf { !it.isNaN() } ?: 0.0,
        profileImageUrl = fotoPerfil
    )
}
