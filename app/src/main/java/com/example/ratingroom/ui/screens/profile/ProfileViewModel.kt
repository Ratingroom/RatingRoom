package com.example.ratingroom.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.repository.ReviewRepository
import com.example.ratingroom.repository.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val _uiState = MutableStateFlow(ProfileUIState(isLoading = false))
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    init {
        loadProfileAndReviews()
    }

    /** <-- wrapper para compatibilidad con MainActivity */
    fun loadProfile() = loadProfileAndReviews()

    fun refresh() = loadProfileAndReviews()

    private fun loadProfileAndReviews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val profileResult = async { authRepository.getUserProfile() }
            val uidHash = authRepository.currentUser?.uid?.hashCode() ?: 0
            val reviewsResult = async { reviewRepository.listByUser(uidHash) }

            val (pRes, rList) = awaitAll(profileResult, reviewsResult)

            (pRes as Result<UserProfile>).onSuccess { userProfile ->
                val memberSinceFormatted = userProfile.createdAt?.let { ts ->
                    try {
                        val date = Date(ts)
                        SimpleDateFormat("MMM yyyy", Locale("es")).format(date)
                    } catch (_: Exception) { null }
                }

                val reviews = (rList as List<com.example.ratingroom.data.dtos.ReviewDto>)
                val count = reviews.size
                val avg = if (count > 0) reviews.map { it.rating }.average() else 0.0

                val profileData = ProfileData(
                    name = userProfile.fullName ?: "Usuario",
                    email = userProfile.email,
                    memberSince = memberSinceFormatted,
                    favoriteGenre = userProfile.favoriteGenre,
                    reviewsCount = count,
                    averageRating = avg,
                    profileImageUrl = userProfile.profileImageUrl,
                    mainMovieId = userProfile.mainMovieId
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profileData = profileData,
                    reviews = reviews
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudo cargar el perfil"
                )
            }
        }
    }

    fun createReview(articuloId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            runCatching {
                reviewRepository.create(currentUserIdForUi(), articuloId, rating, texto)
            }.onSuccess { created ->
                if (created != null) {
                    val newList = listOf(created) + _uiState.value.reviews
                    val count = newList.size
                    val avg = if (count > 0) newList.map { it.rating }.average() else 0.0
                    _uiState.value = _uiState.value.copy(
                        reviews = newList,
                        profileData = _uiState.value.profileData?.copy(
                            reviewsCount = count,
                            averageRating = avg
                        )
                    )
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun updateReview(reviewId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            runCatching {
                reviewRepository.update(currentUserIdForUi(), reviewId.toString(), rating, texto)
            }.onSuccess { updated ->
                if (updated != null) {
                    val newList = _uiState.value.reviews.map { if (it.id == reviewId.toString()) updated else it }
                    val count = newList.size
                    val avg = if (count > 0) newList.map { it.rating }.average() else 0.0
                    _uiState.value = _uiState.value.copy(
                        reviews = newList,
                        profileData = _uiState.value.profileData?.copy(
                            reviewsCount = count,
                            averageRating = avg
                        )
                    )
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            runCatching { reviewRepository.delete(currentUserIdForUi(), reviewId.toString()) }
                .onSuccess { ok ->
                    if (ok) {
                        val newList = _uiState.value.reviews.filterNot { it.id == reviewId.toString() }
                        val count = newList.size
                        val avg = if (count > 0) newList.map { it.rating }.average() else 0.0
                        _uiState.value = _uiState.value.copy(
                            reviews = newList,
                            profileData = _uiState.value.profileData?.copy(
                                reviewsCount = count,
                                averageRating = avg
                            )
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

    private fun currentUserIdForUi(): Int =
        authRepository.currentUser?.uid?.hashCode() ?: 0

    /** <-- esto es lo que MainActivity usa */
    fun logout() {
        authRepository.signOut()
    }
}
