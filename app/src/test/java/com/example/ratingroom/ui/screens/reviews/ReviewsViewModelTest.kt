package com.example.ratingroom.ui.screens.reviews

import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.data.models.Movie
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModelTest {

    private val reviewRepository: com.example.ratingroom.repository.ReviewRepository = mockk(relaxed = true)
    private val movieRepository: com.example.ratingroom.repository.MovieRepository = mockk(relaxed = true)
    private val authRepository: com.example.ratingroom.repository.AuthRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendOrDeleteLike actualiza likes e isLiked en la reseña`() = runTest(testDispatcher) {
        // Arrange: datos iniciales del backend
        val dto = ReviewDto(
            id = "r1",
            usuario_id = 2,
            pelicula_id = 101,
            rating = 4,
            texto = "Muy buena",
            userName = "Juan",
            userImageUrl = null,
            likes = 5
        )
        coEvery { reviewRepository.listByUser(2) } returns Result.success(listOf(dto))
        val movie = Movie(
            id = 101,
            title = "Mi película",
            year = "2020",
            genre = "Drama",
            rating = 4.5,
            reviews = 100,
            description = "Desc",
            director = "Dir",
            duration = "120m",
            imageUrl = null,
            isFavorite = false
        )
        coEvery { movieRepository.getMovieById(101) } returns movie

        // Act: crear VM y dejar que cargue; luego enviar like
        val vm = ReviewsViewModel(reviewRepository, movieRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { reviewRepository.sendOrDeleteLike("r1", any()) } returns Result.success(true)
        vm.sendOrDeleteLike("r1", "uid-123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = vm.uiState.value
        assertThat(state.reviews).isNotEmpty()
        val first = state.reviews.first()
        assertThat(first.id).isEqualTo("r1")
        assertThat(first.likes).isEqualTo(6)
        assertThat(first.isLiked).isTrue()
    }
}