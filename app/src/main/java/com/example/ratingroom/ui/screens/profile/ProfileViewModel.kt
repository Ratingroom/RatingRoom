package com.example.ratingroom.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.ratingroom.data.dtos.UserDto
import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.repository.ReviewRepository
import com.example.ratingroom.data.remote.RetrofitClient
import com.example.ratingroom.repository.UserProfile

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Mantenemos el repositorio REST para las reseñas por ahora
    private val HARDCODED_USER_ID = 2

    private val _uiState = MutableStateFlow(ProfileUIState(isLoading = false))
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile(userId: Int = HARDCODED_USER_ID) {
        Log.d("ProfileViewModel", "=== INICIO loadProfile ===")
        Log.d("ProfileViewModel", "loadProfile() iniciado con userId: $userId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Verificar primero si hay un usuario autenticado en Firebase
            if (authRepository.isUserLoggedIn()) {
                Log.d("ProfileViewModel", "✓ Usuario autenticado en Firebase, cargando desde Firestore primero...")
                loadProfileFromFirebase()
            } else {
                Log.d("ProfileViewModel", "✗ No hay usuario autenticado, intentando REST API...")
                // Intentar con REST API
                runCatching { reviewRepository.getUserProfile(userId) }
                    .onSuccess { prof ->
                        if (prof != null) {
                            Log.d("ProfileViewModel", "✓ Datos obtenidos desde REST API")
                            val reviews = emptyList<com.example.ratingroom.data.dtos.ReviewDto>()
                            val profileData = ProfileData(
                                name = prof.displayName,
                                email = prof.email,
                                memberSince = null,
                                favoriteGenre = prof.favoriteGenre,
                                reviewsCount = 0,
                                averageRating = 0.0,
                                profileImageUrl = prof.profileImageUrl
                            )
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                profileData = profileData,
                                reviews = reviews
                            )
                        } else {
                            Log.d("ProfileViewModel", "✗ REST API retornó null")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "No se pudo cargar el perfil"
                            )
                        }
                    }
                    .onFailure { e ->
                        Log.e("ProfileViewModel", "✗ Error en REST API: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Error al cargar perfil"
                        )
                    }
            }
        }
    }

    private suspend fun loadProfileFromFirebase() {
        Log.d("ProfileViewModel", "Cargando perfil desde Firebase Firestore...")
        authRepository.getUserProfile()
            .onSuccess { userProfile ->
                Log.d("ProfileViewModel", "✓ Datos obtenidos desde Firebase Firestore")
                val profileData = userProfile.toProfileData()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profileData = profileData,
                    reviews = emptyList()
                )
            }
            .onFailure { e ->
                Log.e("ProfileViewModel", "✗ Error al cargar desde Firebase: ${e.message}")
                // Si Firebase falla, intentar con REST API como fallback
                Log.d("ProfileViewModel", "Intentando REST API como fallback...")
                runCatching { reviewRepository.getUserProfile(HARDCODED_USER_ID) }
                    .onSuccess { prof ->
                        if (prof != null) {
                            Log.d("ProfileViewModel", "✓ Datos obtenidos desde REST API (fallback)")
                            val profileData = ProfileData(
                                name = prof.displayName,
                                email = prof.email,
                                memberSince = null,
                                favoriteGenre = prof.favoriteGenre,
                                reviewsCount = 0,
                                averageRating = 0.0,
                                profileImageUrl = prof.profileImageUrl
                            )
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                profileData = profileData,
                                reviews = emptyList()
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "No se pudo cargar el perfil desde ninguna fuente"
                            )
                        }
                    }
                    .onFailure { restApiError ->
                        Log.e("ProfileViewModel", "✗ Error en REST API (fallback): ${restApiError.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "No se pudo cargar el perfil desde ninguna fuente"
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

    // Mapper del DTO del backend REST API a datos de UI
    private fun UserDto.toProfileData() = ProfileData(
        name = displayName,
        email = email,
        memberSince = null,
        favoriteGenre = favoriteGenre,
        reviewsCount = 0, // TODO: Calcular cuando tengamos las reviews
        averageRating = 0.0, // TODO: Calcular cuando tengamos las reviews
        profileImageUrl = profileImageUrl
    )

    // Mapper del UserProfile de Firebase a datos de UI
    private fun UserProfile.toProfileData(): ProfileData {
        val memberSinceFormatted = createdAt?.let { timestamp ->
            try {
                val date = Date(timestamp)
                val formatter = SimpleDateFormat("MMM yyyy", Locale("es"))
                formatter.format(date)
            } catch (e: Exception) {
                null
            }
        }
        
        return ProfileData(
            name = fullName ?: "Usuario",
            email = email,
            memberSince = memberSinceFormatted,
            favoriteGenre = favoriteGenre,
            reviewsCount = 0, // TODO: Calcular cuando tengamos las reviews
            averageRating = 0.0, // TODO: Calcular cuando tengamos las reviews
            profileImageUrl = profileImageUrl
        )
    }

    fun logout() {
        println("ProfileViewModel.logout: Cerrando sesión")
        authRepository.signOut()
    }
}
