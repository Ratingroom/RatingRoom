package com.example.ratingroom.ui.screens.moviedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.models.Review
import com.example.ratingroom.repository.MovieRepository
import com.example.ratingroom.repository.ReviewRepository
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
class MovieDetailViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {

    // se mantiene para compatibilidad con tu UI/DTO
    private val HARDCODED_USER_ID = 2

    private val _uiState = MutableStateFlow(MovieDetailUIState())
    val uiState: StateFlow<MovieDetailUIState> = _uiState.asStateFlow()

    fun loadMovieDetail(movieId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching {
                // ✅ Volvemos a tu método original que SÍ existe
                val movie = movieRepository.getMovieById(movieId)

                // ✅ Reseñas desde Firestore a través del repositorio que ya inyectas
                val reviewsDto = reviewRepository.getReviewsByMovie(movieId)

                // ✅ Mapper DTO -> data.models.Review (tu modelo usa Double en rating)
                val reviews: List<Review> = reviewsDto.map { dto ->
                    Review(
                        id = dto.id,
                        movieId = dto.pelicula_id,
                        userId = dto.usuario_id,
                        rating = dto.rating.toDouble(),
                        comment = dto.texto,
                        date = SimpleDateFormat("dd/MM/yyyy", Locale("es")).format(Date()),
                        // 🎯 Información del usuario autor del review
                        userName = dto.userName,
                        userImageUrl = dto.userImageUrl
                    )
                }

                movie to reviews
            }.onSuccess { (movie: com.example.ratingroom.data.models.Movie?, reviews: List<Review>) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movie = movie,
                    reviews = reviews
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error al cargar detalle"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun createReview(movieId: Int, rating: Int, texto: String) {
        viewModelScope.launch {
            runCatching {
                reviewRepository.create(HARDCODED_USER_ID, movieId, rating, texto)
            }.onSuccess {
                // recarga las reseñas desde Firestore para que aparezca la nueva
                loadMovieDetail(movieId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
}