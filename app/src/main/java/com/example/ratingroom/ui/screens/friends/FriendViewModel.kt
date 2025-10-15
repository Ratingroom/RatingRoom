package com.example.ratingroom.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.repository.ReviewRepository
import com.example.ratingroom.repository.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendUIState())
    val uiState: StateFlow<FriendUIState> = _uiState.asStateFlow()

    private var lastUserId: String? = null

    fun load(userId: String) {
        lastUserId = userId
        viewModelScope.launch {
            // Perfil
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.getUserProfileById(userId)
                .onSuccess { up ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = up.toFriendProfileData()
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "No se pudo cargar el perfil"
                    )
                }

            // Reseñas
            _uiState.value = _uiState.value.copy(isLoadingReviews = true)
            val reviews = runCatching { reviewRepository.getReviewsByUserUid(userId) }
                .getOrElse { emptyList() }
            _uiState.value = _uiState.value.copy(
                isLoadingReviews = false,
                reviews = reviews
            )
        }
    }

    fun retry() {
        lastUserId?.let { load(it) }
    }
}

/* --------- Mapper --------- */

private fun UserProfile.toFriendProfileData(): FriendProfileData {
    val memberSinceFormatted = createdAt?.let { ts ->
        try {
            SimpleDateFormat("MMM yyyy", Locale("es")).format(Date(ts))
        } catch (_: Exception) { null }
    }
    return FriendProfileData(
        uid = uid,
        name = fullName ?: "Usuario",
        email = email,
        memberSince = memberSinceFormatted,
        favoriteGenre = favoriteGenre,
        profileImageUrl = profileImageUrl,
        biography = biography,
        location = location,
        website = website
    )
}
