package com.example.ratingroom.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.repository.ReviewRepository
import com.example.ratingroom.repository.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUIState(isLoading = false))
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    private var followersReg: ListenerRegistration? = null
    private var followingReg: ListenerRegistration? = null

    init {
        loadProfileAndReviews()
    }

    fun loadProfile() = loadProfileAndReviews()

    fun refresh() = loadProfileAndReviews()

    private fun loadProfileAndReviews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                println("ProfileViewModel: Iniciando carga de perfil")
                val profileResult = authRepository.getUserProfile()
                println("ProfileViewModel: Resultado de perfil: ${profileResult.isSuccess}")
                
                val uidHash = authRepository.currentUser?.uid?.hashCode() ?: 0
                val reviewsResult = reviewRepository.listByUser(uidHash)

                if (profileResult.isSuccess && reviewsResult.isSuccess) {
                    val userProfile = profileResult.getOrNull()!!
                    println("ProfileViewModel: Perfil cargado - Name: ${userProfile.fullName}, Email: ${userProfile.email}")
                    val reviews = reviewsResult.getOrNull() ?: emptyList()

                    val memberSinceFormatted = userProfile.createdAt?.let { ts ->
                        try {
                            val date = Date(ts)
                            SimpleDateFormat("MMM yyyy", Locale("es")).format(date)
                        } catch (_: Exception) { null }
                    }

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
                        mainMovieId = userProfile.mainMovieId,
                        followersCount = userProfile.followersCount ?: 0,
                        followingCount = userProfile.followingCount ?: 0
                    )

                    val perfilVerificado = verificarIntegridadDatos(profileData)
                    println("ProfileViewModel: ProfileData creado - ${perfilVerificado.name}")

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profileData = perfilVerificado,
                        reviews = reviews
                    )

                    observeUserReviewsRealTime()
                    observeFollowCounts()
                } else {
                    val error = profileResult.exceptionOrNull() ?: reviewsResult.exceptionOrNull()
                    println("ProfileViewModel: Error al cargar perfil: ${error?.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error?.message ?: "No se pudo cargar el perfil"
                    )
                }
            } catch (e: Exception) {
                println("ProfileViewModel: Excepción al cargar perfil: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudo cargar el perfil"
                )
            }
        }
    }

    private fun observeUserReviewsRealTime() {
        val userUid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            reviewRepository.observeReviewsByUserUid(userUid).collectLatest { reviewsDto ->
                val count = reviewsDto.size
                val avg = if (count > 0) reviewsDto.map { it.rating }.average() else 0.0

                _uiState.value = _uiState.value.copy(
                    reviews = reviewsDto,
                    profileData = _uiState.value.profileData?.copy(
                        reviewsCount = count,
                        averageRating = avg
                    )
                )
            }
        }
    }

    private fun observeFollowCounts() {
        clearFollowListeners()
        val uid = authRepository.currentUser?.uid ?: return

        followersReg = firestore.collection("users")
            .document(uid)
            .collection("followers")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("ProfileVM", "followers listener error: ${err.message}", err)
                    return@addSnapshotListener
                }
                val newCount = snap?.size() ?: 0
                _uiState.value = _uiState.value.copy(
                    profileData = _uiState.value.profileData?.copy(followersCount = newCount)
                )
            }

        followingReg = firestore.collection("users")
            .document(uid)
            .collection("following")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("ProfileVM", "following listener error: ${err.message}", err)
                    return@addSnapshotListener
                }
                val newCount = snap?.size() ?: 0
                _uiState.value = _uiState.value.copy(
                    profileData = _uiState.value.profileData?.copy(followingCount = newCount)
                )
            }
    }

    private fun clearFollowListeners() {
        followersReg?.remove()
        followingReg?.remove()
        followersReg = null
        followingReg = null
    }

    fun createReview(articuloId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            try {
                val result = reviewRepository.create(currentUserIdForUi(), articuloId, rating, texto)
                if (result.isSuccess) {
                    val created = result.getOrNull()
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
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun updateReview(reviewId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            try {
                val result = reviewRepository.update(currentUserIdForUi(), reviewId.toString(), rating, texto)
                if (result.isSuccess) {
                    val updated = result.getOrNull()
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
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            try {
                val result = reviewRepository.delete(currentUserIdForUi(), reviewId.toString())
                if (result.isSuccess) {
                    val ok = result.getOrNull() ?: false
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
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
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

    fun logout() {
        clearFollowListeners()
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    // ---------- Función en línea: mejora integridad ----------
    private fun verificarIntegridadDatos(profile: ProfileData): ProfileData {
        var datos = profile

        // Validar nombre
        if (datos.name.isBlank() || datos.name == "Usuario") {
            datos = datos.copy(name = "Usuario sin nombre")
        }

        // Validar email
        if (datos.email.isNullOrBlank() || !datos.email.contains("@")) {
            datos = datos.copy(email = "email_invalido@ratingroom.com")
        }

        // Validar valores negativos o nulos
        val followers = if (datos.followersCount < 0) 0 else datos.followersCount
        val following = if (datos.followingCount < 0) 0 else datos.followingCount
        val avgRating = if (datos.averageRating.isNaN()) 0.0 else datos.averageRating

        return datos.copy(
            followersCount = followers,
            followingCount = following,
            averageRating = avgRating
        )
    }

    // ---------- Seguidores / Seguidos ----------
    fun loadFollowers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFollowers = true) }
            val result = authRepository.getFollowers()
            if (result.isSuccess) {
                _uiState.update { it.copy(
                    followers = result.getOrNull() ?: emptyList(),
                    isLoadingFollowers = false
                ) }
            } else {
                _uiState.update { it.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "No se pudieron cargar los seguidores",
                    isLoadingFollowers = false
                ) }
            }
        }
    }

    fun loadFollowing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFollowing = true) }
            val result = authRepository.getFollowing()
            if (result.isSuccess) {
                _uiState.update { it.copy(
                    following = result.getOrNull() ?: emptyList(),
                    isLoadingFollowing = false
                ) }
            } else {
                _uiState.update { it.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "No se pudieron cargar los usuarios seguidos",
                    isLoadingFollowing = false
                ) }
            }
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                val success = authRepository.followUser(userId)
                if (success) loadFollowing()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "No se pudo seguir al usuario"
                )
            }
        }
    }

    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            try {
                val success = authRepository.unfollowUser(userId)
                if (success) loadFollowing()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "No se pudo dejar de seguir al usuario"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearFollowListeners()
    }
}
