package com.example.ratingroom.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.repository.AuthRepository
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import java.util.UUID

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUIState())
    val uiState: StateFlow<EditProfileUIState> = _uiState.asStateFlow()

    init {
        loadCurrentProfile()
    }

    /** Carga el perfil actual desde Firestore y pre-llena el formulario. */
    private fun loadCurrentProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.getUserProfile()
            
            if (result.isSuccess) {
                val userProfile = result.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    displayName = userProfile?.fullName ?: "",
                    email = userProfile?.email ?: "",
                    biography = userProfile?.biography ?: "",
                    location = userProfile?.location ?: "",
                    favoriteGenre = userProfile?.favoriteGenre ?: "",
                    birthdate = userProfile?.birthdate ?: "",
                    website = userProfile?.website ?: "",
                    profileImageUri = userProfile?.profileImageUrl
                )
            } else {
                val error = result.exceptionOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error?.message ?: "No se pudo cargar el perfil"
                )
            }
        }
    }

    /* -------------------- Handlers de formulario -------------------- */

    fun onDisplayNameChange(displayName: String) {
        _uiState.value = _uiState.value.copy(displayName = displayName, errorMessage = null)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onBiographyChange(biography: String) {
        _uiState.value = _uiState.value.copy(biography = biography, errorMessage = null)
    }

    fun onLocationChange(location: String) {
        _uiState.value = _uiState.value.copy(location = location, errorMessage = null)
    }

    fun onFavoriteGenreChange(favoriteGenre: String) {
        _uiState.value = _uiState.value.copy(favoriteGenre = favoriteGenre, errorMessage = null)
    }

    fun onBirthdateChange(birthdate: String) {
        _uiState.value = _uiState.value.copy(birthdate = birthdate, errorMessage = null)
    }

    fun onWebsiteChange(website: String) {
        _uiState.value = _uiState.value.copy(website = website, errorMessage = null)
    }

    /** Guarda en estado un URI (String) para previsualización y subida. */
    fun onProfileImageSelected(uri: Uri) {
        println("Imagen seleccionada en onProfileImageSelected: $uri")
        _uiState.value = _uiState.value.copy(profileImageUri = uri.toString(), errorMessage = null)
    }

    /* -------------------- Storage: subir imagen -------------------- */

    private suspend fun uploadProfileImage(uri: Uri): String? {
        return withContext(NonCancellable + Dispatchers.IO) {
            try {
                println("uploadProfileImage: Iniciando subida de imagen a Firebase Storage")
                val user = authRepository.currentUser ?: throw IllegalStateException("Usuario no autenticado")
                println("uploadProfileImage: Usuario autenticado: ${user.uid}")

                if (uri.scheme == null) {
                    throw IllegalArgumentException("URI inválida: no tiene scheme")
                }

                val fileRef = storage.reference.child("profile_images/${user.uid}/${UUID.randomUUID()}.jpg")
                println("uploadProfileImage: putFile() -> ${fileRef.path}")
                val uploadTask = fileRef.putFile(uri).await()
                println("uploadProfileImage: Subido OK, bytes: ${uploadTask.bytesTransferred}")

                val downloadUrl = fileRef.downloadUrl.await().toString()
                if (downloadUrl.isEmpty()) throw IllegalStateException("La URL de descarga está vacía")
                println("uploadProfileImage: URL => $downloadUrl")
                downloadUrl
            } catch (e: Exception) {
                println("uploadProfileImage: ERROR -> ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al subir la imagen: ${e.message}"
                    )
                }
                null
            }
        }
    }

    /* -------------------- Guardar perfil (Firestore + Auth) -------------------- */

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            println("SaveProfile: Iniciando guardado del perfil")

            try {
                val currentState = _uiState.value

                // Validaciones mínimas
                when {
                    currentState.displayName.isBlank() -> {
                        _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "El nombre para mostrar es requerido")
                        return@launch
                    }
                    currentState.email.isBlank() || !currentState.email.contains("@") -> {
                        _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "Email inválido")
                        return@launch
                    }
                }

                // Subir imagen si hay nueva seleccionada (URI con scheme content:// o file://)
                var profileImageUrl: String? = null
                val selectedUriStr = currentState.profileImageUri
                if (!selectedUriStr.isNullOrBlank()) {
                    // Si lo que hay es una URL http(s) previa, no la resubimos
                    val isHttp = selectedUriStr.startsWith("http://") || selectedUriStr.startsWith("https://")
                    if (!isHttp) {
                        val uri = Uri.parse(selectedUriStr)
                        profileImageUrl = uploadProfileImage(uri)
                        if (profileImageUrl.isNullOrEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                errorMessage = "No se pudo obtener la URL de la imagen"
                            )
                            return@launch
                        }
                    } else {
                        // Ya había URL remota, no hay nueva imagen — no actualizar
                        profileImageUrl = null
                    }
                }

                // Evitar re-login innecesario: solo actualiza email si cambia
                val normalizedEmail = currentState.email.trim().lowercase()
                val currentEmail = authRepository.currentUser?.email?.trim()?.lowercase()
                val emailToUpdate: String? = if (normalizedEmail.isNotEmpty() && normalizedEmail != currentEmail) {
                    normalizedEmail
                } else null

                println("SaveProfile: Actualizando perfil. emailToUpdate=${emailToUpdate ?: "(sin cambio)"}")

                try {
                    authRepository.updateUserProfile(
                        displayName = currentState.displayName,
                        email = emailToUpdate,
                        biography = currentState.biography,
                        location = currentState.location,
                        favoriteGenre = currentState.favoriteGenre,
                        birthdate = currentState.birthdate,
                        website = currentState.website,
                        profileImageUrl = profileImageUrl
                    )
                    
                    loadCurrentProfile()
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveCompleted = true,
                        successMessage = "Perfil actualizado exitosamente"
                    )
                } catch (e: Exception) {
                    println("Error al actualizar perfil: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Error al actualizar perfil"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Error al guardar"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            saveCompleted = false
        )
    }
}
