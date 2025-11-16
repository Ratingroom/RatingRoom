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

            val profileResult = async { authRepository.getUserProfile() }
            val uidHash = authRepository.currentUser?.uid?.hashCode() ?: 0
            val reviewsResult = async { reviewRepository.listByUser(uidHash) }

            val (pResAny, rResAny) = awaitAll(profileResult, reviewsResult)

            val pRes = pResAny as Result<UserProfile>
            val rRes = rResAny as Result<List<com.example.ratingroom.data.dtos.ReviewDto>>

            pRes.onSuccess { userProfile ->
                val memberSinceFormatted = userProfile.createdAt?.let { ts ->
                    try {
                        val date = Date(ts)
                        SimpleDateFormat("MMM yyyy", Locale("es")).format(date)
                    } catch (_: Exception) { null }
                }

                val reviews = rRes.getOrElse { emptyList() }
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

                // ✅ Verificar integridad básica de los datos del usuario
                val perfilVerificado = verificarIntegridadDatos(profileData)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profileData = perfilVerificado,
                    reviews = reviews
                )

                observeUserReviewsRealTime()
                observeFollowCounts()
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
            reviewRepository.create(currentUserIdForUi(), articuloId, rating, texto)
                .onSuccess { created ->
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
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
        }
    }

    fun updateReview(reviewId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            reviewRepository.update(currentUserIdForUi(), reviewId.toString(), rating, texto)
                .onSuccess { updated ->
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
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            reviewRepository.delete(currentUserIdForUi(), reviewId.toString())
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

    fun onDarkModeChange(isDarkMode: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = isDarkMode)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun currentUserIdForUi(): Int =
        authRepository.currentUser?.uid?.hashCode() ?: 0

    fun logout(context: android.content.Context, onComplete: () -> Unit) {
        clearFollowListeners()
        viewModelScope.launch {
            try {
                authRepository.signOut(context)
                clearAllState()
                onComplete()
            } catch (e: Exception) {
                // Aún así limpiar estado y completar el logout en la UI
                clearAllState()
                onComplete()
            }
        }
    }
    
    /**
     * Limpia todo el estado del ViewModel al hacer logout
     */
    private fun clearAllState() {
        _uiState.value = ProfileUIState(isLoading = false)
        _followers.value = emptyList()
        _following.value = emptyList()
        _isLoadingFollowers.value = false
        _isLoadingFollowing.value = false
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
    private val _followers = MutableStateFlow<List<UserProfile>>(emptyList())
    val followers: StateFlow<List<UserProfile>> = _followers.asStateFlow()

    private val _following = MutableStateFlow<List<UserProfile>>(emptyList())
    val following: StateFlow<List<UserProfile>> = _following.asStateFlow()

    private val _isLoadingFollowers = MutableStateFlow(false)
    val isLoadingFollowers: StateFlow<Boolean> = _isLoadingFollowers.asStateFlow()

    private val _isLoadingFollowing = MutableStateFlow(false)
    val isLoadingFollowing: StateFlow<Boolean> = _isLoadingFollowing.asStateFlow()

    fun loadFollowers() {
        viewModelScope.launch {
            _isLoadingFollowers.value = true
            authRepository.getFollowers()
                .onSuccess { followersList ->
                    _followers.value = followersList
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message ?: "No se pudieron cargar los seguidores"
                    )
                }
            _isLoadingFollowers.value = false
        }
    }

    fun loadFollowing() {
        viewModelScope.launch {
            _isLoadingFollowing.value = true
            authRepository.getFollowing()
                .onSuccess { followingList ->
                    _following.value = followingList
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message ?: "No se pudieron cargar los usuarios seguidos"
                    )
                }
            _isLoadingFollowing.value = false
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            authRepository.followUser(userId)
                .onSuccess { success ->
                    if (success) loadFollowing()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message ?: "No se pudo seguir al usuario"
                    )
                }
        }
    }

    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            authRepository.unfollowUser(userId)
                .onSuccess { success ->
                    if (success) loadFollowing()
                }
                .onFailure { e ->
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
