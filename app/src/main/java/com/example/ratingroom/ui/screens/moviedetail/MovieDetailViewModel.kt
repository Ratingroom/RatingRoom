package com.example.ratingroom.ui.screens.moviedetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.models.Review
import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.repository.MovieRepository
import com.example.ratingroom.repository.ReviewRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val movieRepository: MovieRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val HARDCODED_USER_ID = 2

    private val _uiState = MutableStateFlow(MovieDetailUIState())
    val uiState: StateFlow<MovieDetailUIState> = _uiState.asStateFlow()

    fun setShowFollowingOnly(enabled: Boolean) {
        _uiState.update { it.copy(showFollowingOnly = enabled) }
        if (enabled) refreshFollowingNames()
        _uiState.update { st -> st.copy(reviews = applyFilterIfNeeded(st.reviews)) }
    }

    fun loadMovieDetail(movieId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            observeMyLikes()

            try {
                val movie = movieRepository.getMovieById(movieId)
                val reviewsResult = reviewRepository.getReviewsByMovie(movieId)
                
                if (reviewsResult.isSuccess) {
                    val reviewsDto = reviewsResult.getOrNull() ?: emptyList()
                    val mapped = mapReviewsFromDto(reviewsDto, _uiState.value.likedIds)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        movie = movie,
                        reviews = applyFilterIfNeeded(mapped)
                    )
                    observeReviewsRealTime(movieId)
                } else {
                    val error = reviewsResult.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error?.message ?: "Error al cargar detalle"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error al cargar detalle"
                )
            }
        }
    }

    private fun observeReviewsRealTime(movieId: Int) {
        viewModelScope.launch {
            reviewRepository.observeReviewsByMovie(movieId).collectLatest { reviewsDto ->
                val all = mapReviewsFromDto(reviewsDto, _uiState.value.likedIds)
                val filtered = if (!_uiState.value.showFollowingOnly) all 
                    else filterByFollowingNames(all, _uiState.value.followingNames)
                _uiState.update { it.copy(reviews = filtered) }
            }
        }
    }

    private fun observeMyLikes() {
        val uid = getCurrentUserId()
        if (uid.isBlank() || uid == "anonymous") {
            _uiState.update { it.copy(likedIds = emptySet()) }
            return
        }
        firestore.collection("users")
            .document(uid)
            .collection("likes")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("MovieDetailVM", "Error observando likes: ${err.message}", err)
                    return@addSnapshotListener
                }
                if (snap != null) {
                    val likedIds = snap.documents.map { it.id }.toSet()
                    _uiState.update { st ->
                        st.copy(
                            likedIds = likedIds,
                            reviews = st.reviews.map { r -> r.copy(isLiked = r.id in likedIds) }
                        )
                    }
                }
            }
    }

    private fun applyFilterIfNeeded(all: List<Review>): List<Review> {
        return if (!_uiState.value.showFollowingOnly) all
        else filterByFollowingNames(all, _uiState.value.followingNames)
    }

    private fun filterByFollowingNames(
        all: List<Review>,
        followingNames: Set<String>
    ): List<Review> {
        val myNumericId = HARDCODED_USER_ID
        val myDisplayName = authRepository.currentUser?.displayName?.trim()

        return all.filter { r ->
            (r.userId == myNumericId) ||
                    (!myDisplayName.isNullOrEmpty() && r.userName?.trim() == myDisplayName) ||
                    (r.userName != null && r.userName in followingNames)
        }
    }

    private fun refreshFollowingNames() {
        val currentUid = getCurrentUserId()
        if (currentUid.isBlank() || currentUid == "anonymous") {
            _uiState.update { it.copy(followingNames = emptySet()) }
            return
        }
        viewModelScope.launch {
            try {
                val followingSnap = firestore.collection("users")
                    .document(currentUid)
                    .collection("following")
                    .get()
                    .await()

                val followingIds = followingSnap.documents.map { it.id }
                if (followingIds.isEmpty()) {
                    _uiState.update { st -> 
                        st.copy(
                            followingNames = emptySet(),
                            reviews = applyFilterIfNeeded(st.reviews)
                        )
                    }
                    return@launch
                }

                val names = mutableSetOf<String>()
                followingIds.chunked(10).forEach { chunk ->
                    val profiles = firestore.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    profiles.documents.forEach { doc ->
                        val name = (doc.get("fullName") as? String)?.trim()
                        if (!name.isNullOrEmpty()) names.add(name)
                    }
                }

                _uiState.update { st -> 
                    st.copy(
                        followingNames = names,
                        reviews = applyFilterIfNeeded(st.reviews)
                    )
                }
            } catch (e: Exception) {
                Log.e("MovieDetailVM", "Error cargando seguidos: ${e.message}", e)
                _uiState.update { st -> 
                    st.copy(
                        followingNames = emptySet(),
                        reviews = applyFilterIfNeeded(st.reviews)
                    )
                }
            }
        }
    }

    private fun mapReviewsFromDto(
        reviewsDto: List<com.example.ratingroom.data.dtos.ReviewDto>,
        likedIds: Set<String>
    ): List<Review> {
        return reviewsDto.map { dto ->
            Review(
                id = dto.id,
                movieId = dto.pelicula_id,
                userId = dto.usuario_id,
                rating = dto.rating.toDouble(),
                comment = dto.texto,
                date = SimpleDateFormat("dd/MM/yyyy", Locale("es")).format(Date()),
                userName = dto.userName,
                userImageUrl = dto.userImageUrl,
                likes = (dto.likes.takeIf { it >= 0 } ?: 0),
                isLiked = dto.id in likedIds
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun createReview(movieId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            try {
                val result = reviewRepository.create(HARDCODED_USER_ID, movieId, rating, texto)
                if (result.isSuccess) {
                    loadMovieDetail(movieId)
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

    fun getCurrentUserId(): String = authRepository.currentUser?.uid ?: "anonymous"

    // ⬇️ FIX: no tocamos el contador; solo reflejamos isLiked.
    fun sendOrDeleteLike(reviewId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = reviewRepository.sendOrDeleteLike(reviewId, userId)
                if (result.isSuccess) {
                    val wasLiked = result.getOrNull() ?: false
                    _uiState.update { current ->
                        current.copy(
                            reviews = current.reviews.map { r ->
                                if (r.id == reviewId) r.copy(isLiked = wasLiked) else r
                            }
                        )
                    }
                    // El contador "likes" lo actualizará el listener en tiempo real.
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
}
