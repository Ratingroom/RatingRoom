package com.example.ratingroom.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// TODO: Firebase - Comentado temporalmente para usar solo REST API
// import com.example.ratingroom.Config.CURRENT_USER_ID// import com.example.ratingroom.Config.CURRENT_USER_ID// import com.example.ratingroom.Config.CURRENT_USER_ID
import com.example.ratingroom.data.dtos.UserDto
// TODO: Firebase - Comentado temporalmente para usar solo REST API
// import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.data.services.ReviewApiService
import com.example.ratingroom.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
    // TODO: Firebase - Comentado temporalmente para usar solo REST API
    // private val authRepository: AuthRepository
) : ViewModel() {

    // ID de usuario quemado para obtener datos de REST API
    private val HARDCODED_USER_ID = 2

    private val _uiState = MutableStateFlow(ProfileUIState(isLoading = true))
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    init {
        loadProfile(HARDCODED_USER_ID)
    }

    fun loadProfile(userId: Int = HARDCODED_USER_ID) {
        Log.d("ProfileViewModel", "=== INICIO loadProfile ===")
        Log.d("ProfileViewModel", "loadProfile() iniciado con userId: $userId (HARDCODED_USER_ID: $HARDCODED_USER_ID)")
        Log.d("ProfileViewModel", "¿userId == HARDCODED_USER_ID? ${userId == HARDCODED_USER_ID}")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            Log.d("ProfileViewModel", "Llamando reviewRepository.getUserProfile($userId)")
            runCatching { reviewRepository.getUserProfile(userId) }
                .onSuccess { prof ->
                    Log.d("ProfileViewModel", "=== RESPUESTA EXITOSA ===")
                    Log.d("ProfileViewModel", "Respuesta recibida: $prof")
                    Log.d("ProfileViewModel", "ID del usuario recibido: ${prof?.id}")
                    Log.d("ProfileViewModel", "Nombre del usuario recibido: ${prof?.displayName}")
                    Log.d("ProfileViewModel", "Email del usuario recibido: ${prof?.email}")
                    // TODO: Obtener reviews por separado cuando esté disponible
                    val reviews = emptyList<com.example.ratingroom.data.dtos.ReviewDto>()
                    val profileData = prof?.let { user ->
                        ProfileData(
                            name = user.displayName,
                            email = user.email,
                            memberSince = null,
                            favoriteGenre = user.favoriteGenre,
                            reviewsCount = 0, // TODO: Calcular cuando tengamos las reviews
                            averageRating = 0.0, // TODO: Calcular cuando tengamos las reviews
                            profileImageUrl = user.profileImageUrl
                        )
                    }
                    Log.d("ProfileViewModel", "ProfileData mapeado: $profileData")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profileData = profileData,
                        reviews = reviews
                    )
                    Log.d("ProfileViewModel", "=== ESTADO ACTUALIZADO ===")
                }
                .onFailure { e ->
                    Log.e("ProfileViewModel", "=== ERROR ===")
                    Log.e("ProfileViewModel", "Error al cargar perfil: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar perfil"
                    )
                }
        }
    }

    fun createReview(articuloId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            runCatching { reviewRepository.create(HARDCODED_USER_ID, articuloId, rating, texto) }
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
            runCatching { reviewRepository.update(HARDCODED_USER_ID, reviewId, rating, texto) }
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
            runCatching { reviewRepository.delete(HARDCODED_USER_ID, reviewId) }
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
    private fun UserDto.toProfileData() = ProfileData(
        name = displayName,
        email = email,
        memberSince = null,
        favoriteGenre = favoriteGenre,
        reviewsCount = 0, // TODO: Calcular cuando tengamos las reviews
        averageRating = 0.0, // TODO: Calcular cuando tengamos las reviews
        profileImageUrl = profileImageUrl
    )

    fun logout() {
        println("ProfileViewModel.logout: Cerrando sesión")
        // TODO: Firebase - Comentado temporalmente para usar solo REST API
        // authRepository.signOut()
    }
}
