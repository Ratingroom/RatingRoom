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

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authRepository.getUserProfile()
                .onSuccess { userProfile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        displayName = userProfile.fullName ?: "",
                        email = userProfile.email,
                        biography = userProfile.biography ?: "",
                        location = userProfile.location ?: "",
                        favoriteGenre = userProfile.favoriteGenre ?: "",
                        birthdate = userProfile.birthdate ?: "",
                        website = userProfile.website ?: ""
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "No se pudo cargar el perfil"
                    )
                }
        }
    }

    fun onDisplayNameChange(displayName: String) {
        _uiState.value = _uiState.value.copy(
            displayName = displayName,
            errorMessage = null
        )
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            errorMessage = null
        )
    }

    fun onBiographyChange(biography: String) {
        _uiState.value = _uiState.value.copy(
            biography = biography,
            errorMessage = null
        )
    }

    fun onLocationChange(location: String) {
        _uiState.value = _uiState.value.copy(
            location = location,
            errorMessage = null
        )
    }

    fun onFavoriteGenreChange(favoriteGenre: String) {
        _uiState.value = _uiState.value.copy(
            favoriteGenre = favoriteGenre,
            errorMessage = null
        )
    }

    fun onBirthdateChange(birthdate: String) {
        _uiState.value = _uiState.value.copy(
            birthdate = birthdate,
            errorMessage = null
        )
    }

    fun onProfileImageSelected(uri: Uri) {
        println("Imagen seleccionada en onProfileImageSelected: $uri")
        val uriString = uri.toString()
        println("URI convertida a String: $uriString")
        _uiState.value = _uiState.value.copy(
            profileImageUri = uriString,
            errorMessage = null
        )
        println("Estado actualizado con profileImageUri: ${_uiState.value.profileImageUri}")
    }

    private suspend fun uploadProfileImage(uri: Uri): String? {
        return withContext(NonCancellable + Dispatchers.IO) {
            try {
                println("uploadProfileImage: Iniciando subida de imagen a Firebase Storage")
                val user = authRepository.currentUser ?: throw IllegalStateException("Usuario no autenticado")
                println("uploadProfileImage: Usuario autenticado: ${user.uid}")

                if (uri.scheme == null) {
                    println("uploadProfileImage: ERROR - URI inválida: no tiene scheme")
                    throw IllegalArgumentException("URI inválida: no tiene scheme")
                }

                println("uploadProfileImage: URI a subir: $uri")
                println("uploadProfileImage: URI scheme: ${uri.scheme}, path: ${uri.path}")

                val fileRef = storage.reference.child("profile_images/${user.uid}/${UUID.randomUUID()}.jpg")
                println("uploadProfileImage: Referencia de archivo creada: ${fileRef.path}")

                println("uploadProfileImage: Iniciando putFile...")
                try {
                    println("uploadProfileImage: Ejecutando putFile y esperando resultado...")
                    val uploadTask = fileRef.putFile(uri).await()
                    println("uploadProfileImage: Archivo subido exitosamente, bytes transferidos: ${uploadTask.bytesTransferred}")
                    println("uploadProfileImage: Obteniendo URL de descarga...")

                    val downloadUrl = fileRef.downloadUrl.await()
                    val downloadUrlString = downloadUrl.toString()
                    println("uploadProfileImage: URL de descarga obtenida: $downloadUrlString")

                    if (downloadUrlString.isEmpty()) {
                        println("uploadProfileImage: ADVERTENCIA - URL de descarga está vacía")
                        throw IllegalStateException("La URL de descarga está vacía")
                    }

                    downloadUrlString
                } catch (e: Exception) {
                    println("uploadProfileImage: ERROR específico en putFile o downloadUrl: ${e.message}")
                    println("uploadProfileImage: Stacktrace del error:")
                    e.printStackTrace()
                    throw e
                }
            } catch (e: Exception) {
                println("uploadProfileImage: ERROR general al subir imagen: ${e.message}")
                println("uploadProfileImage: Stacktrace del error general:")
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

    fun onWebsiteChange(website: String) {
        _uiState.value = _uiState.value.copy(
            website = website,
            errorMessage = null
        )
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            println("SaveProfile: Iniciando guardado del perfil")

            try {
                val currentState = _uiState.value

                when {
                    currentState.displayName.isBlank() -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "El nombre para mostrar es requerido"
                        )
                        println("SaveProfile: Error - El nombre no puede estar vacío")
                        return@launch
                    }
                    currentState.email.isBlank() || !currentState.email.contains("@") -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "Email inválido"
                        )
                        println("SaveProfile: Error - Email inválido")
                        return@launch
                    }
                }

                // Subir imagen si hay una seleccionada
                var profileImageUrl: String? = null
                println("SaveProfile: Verificando si hay imagen para subir")
                println("SaveProfile: profileImageUri = ${currentState.profileImageUri}")

                if (currentState.profileImageUri != null) {
                    println("SaveProfile: Procesando URI: ${currentState.profileImageUri}")
                    try {
                        val uri = Uri.parse(currentState.profileImageUri)
                        println("SaveProfile: URI parseada correctamente: $uri")
                        println("SaveProfile: URI scheme: ${uri.scheme}, path: ${uri.path}")

                        if (uri.scheme == null || uri.path == null) {
                            println("SaveProfile: ERROR: URI inválida, no tiene scheme o path")
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                errorMessage = "URI de imagen inválida"
                            )
                            return@launch
                        }

                        println("SaveProfile: URI válido, subiendo imagen...")
                        val uploadedImageUrl = uploadProfileImage(uri)

                        if (!uploadedImageUrl.isNullOrEmpty()) {
                            println("SaveProfile: URL de imagen obtenida correctamente: $uploadedImageUrl")
                            profileImageUrl = uploadedImageUrl
                        } else {
                            println("SaveProfile: ADVERTENCIA - No se pudo obtener URL de imagen")
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                errorMessage = "No se pudo obtener la URL de la imagen"
                            )
                            return@launch
                        }

                        println("SaveProfile: URL de imagen después de subir: $profileImageUrl")
                    } catch (e: Exception) {
                        println("SaveProfile: Error al procesar URI: ${e.message}")
                        e.printStackTrace()
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "Error al procesar la imagen: ${e.message}"
                        )
                        return@launch
                    }
                } else {
                    println("SaveProfile: No hay imagen seleccionada para subir")
                }

                // Guardar perfil usando AuthRepository (ahora retorna Result<Unit>)
                println("Actualizando perfil con profileImageUrl: $profileImageUrl")
                authRepository.updateUserProfile(
                    displayName = currentState.displayName,
                    email = currentState.email,
                    biography = currentState.biography,
                    location = currentState.location,
                    favoriteGenre = currentState.favoriteGenre,
                    birthdate = currentState.birthdate,
                    website = currentState.website,
                    profileImageUrl = profileImageUrl
                )
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveCompleted = true,
                            successMessage = "Perfil actualizado exitosamente"
                        )
                    }
                    .onFailure { e ->
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
