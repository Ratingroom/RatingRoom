package com.example.ratingroom.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.models.Review
import com.example.ratingroom.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    // ID del usuario actual (quemado como en los requisitos)
    private val currentUserId = 1

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            println("ProfileViewModel.loadProfile: Iniciando carga de perfil")

            authRepository.getUserProfile()
                .onSuccess { userProfile ->
                    println("ProfileViewModel.loadProfile: Perfil cargado con éxito")
                    println("ProfileViewModel.loadProfile: profileImageUrl: ${userProfile.profileImageUrl}")

                    val imageUrl = userProfile.profileImageUrl?.takeIf { it.isNotEmpty() }
                    val profileData = ProfileData(
                        name = userProfile.fullName ?: "Usuario",
                        email = userProfile.email,
                        memberSince = "Enero 2024", // TODO: calcular desde createdAt si aplica
                        favoriteGenre = userProfile.favoriteGenre ?: "No especificado",
                        reviewsCount = 0,           // TODO: origen real
                        averageRating = 0.0,        // TODO: origen real
                        profileImageUrl = imageUrl
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profileData = profileData
                    )
                    println("ProfileViewModel.loadProfile: Estado actualizado con profileImageUrl: ${_uiState.value.profileData?.profileImageUrl}")
                    
                    // Cargar las reseñas del usuario después de cargar el perfil
                    loadUserReviews()
                }
                .onFailure { e ->
                    println("ProfileViewModel.loadProfile: ERROR al cargar perfil: ${e.message}")
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

    fun onDarkModeChange(isDarkMode: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = isDarkMode)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun refreshProfile() {
        println("ProfileViewModel.refreshProfile: Refrescando perfil")
        loadProfile()
    }

    fun logout() {
        println("ProfileViewModel.logout: Cerrando sesión")
        authRepository.signOut()
    }
}
