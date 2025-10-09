package com.example.ratingroom.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.repository.AuthRepository
import com.example.ratingroom.data.remote.RetrofitClient
import com.example.ratingroom.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Mantenemos el repositorio REST para las reseñas por ahora
    private val HARDCODED_USER_ID = 2
    private val repo = ReviewRepository(RetrofitClient.reviewApi)

    private val _uiState = MutableStateFlow(ProfileUIState(isLoading = true))
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        Log.d("ProfileViewModel", "=== INICIO loadProfile con Firebase ===")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Cargar perfil desde Firebase
            authRepository.getUserProfile()
                .onSuccess { userProfile ->
                    Log.d("ProfileViewModel", "=== PERFIL FIREBASE CARGADO ===")
                    Log.d("ProfileViewModel", "Usuario: ${userProfile.fullName}")
                    Log.d("ProfileViewModel", "Email: ${userProfile.email}")
                    
                    val profileData = ProfileData(
                        name = userProfile.fullName ?: userProfile.email.substringBefore("@"),
                        email = userProfile.email,
                        memberSince = null, // Puedes calcular esto desde createdAt si lo necesitas
                        favoriteGenre = userProfile.favoriteGenre,
                        reviewsCount = 0, // Por ahora, las reseñas siguen viniendo del REST API
                        averageRating = 0.0,
                        profileImageUrl = userProfile.profileImageUrl
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profileData = profileData,
                        reviews = emptyList() // Por ahora vacío, las reseñas siguen en REST API
                    )
                    Log.d("ProfileViewModel", "=== ESTADO FIREBASE ACTUALIZADO ===")
                }
                .onFailure { e ->
                    Log.e("ProfileViewModel", "=== ERROR FIREBASE ===")
                    Log.e("ProfileViewModel", "Error al cargar perfil Firebase: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar perfil"
                    )
                }
        }
    }

    fun createReview(articuloId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            runCatching { repo.create(HARDCODED_USER_ID, articuloId, rating, texto) }
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
            runCatching { repo.update(HARDCODED_USER_ID, reviewId, rating, texto) }
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
            runCatching { repo.delete(HARDCODED_USER_ID, reviewId) }
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

    // Mapper del DTO del backend a datos de UI (ya no se usa con Firebase)
    // private fun UserProfileDto.toProfileData() = ProfileData(
    //     name = nombre?.takeIf { it.isNotBlank() } ?: username,
    //     email = email,
    //     memberSince = null,
    //     favoriteGenre = null,
    //     reviewsCount = reviews.size,
    //     averageRating = reviews.map { it.rating }.average().takeIf { !it.isNaN() } ?: 0.0,
    //     profileImageUrl = fotoPerfil
    // )

    fun logout() {
        println("ProfileViewModel.logout: Cerrando sesión con Firebase")
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
