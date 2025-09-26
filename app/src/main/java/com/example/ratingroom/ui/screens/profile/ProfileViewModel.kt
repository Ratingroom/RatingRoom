package com.example.ratingroom.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.repository.AuthRepository
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

    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

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
