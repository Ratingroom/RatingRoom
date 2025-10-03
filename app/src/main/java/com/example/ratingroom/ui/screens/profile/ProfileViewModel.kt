package com.example.ratingroom.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class ProfileViewModel @Inject constructor(
) : ViewModel() {

    private val repo = ReviewRepository(RetrofitClient.reviewApi)

    private val _uiState = MutableStateFlow(ProfileUIState(isLoading = true))
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

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
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar perfil"
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
