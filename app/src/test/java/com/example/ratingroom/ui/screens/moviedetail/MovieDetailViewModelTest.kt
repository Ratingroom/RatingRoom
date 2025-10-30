package com.example.ratingroom.ui.screens.moviedetail

import com.example.ratingroom.data.models.Movie
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {

    private val movieRepository: com.example.ratingroom.repository.MovieRepository = mockk(relaxed = true)
    private val reviewRepository: com.example.ratingroom.repository.ReviewRepository = mockk(relaxed = true)
    private val authRepository: com.example.ratingroom.repository.AuthRepository = mockk(relaxed = true)
    private val firestore: com.google.firebase.firestore.FirebaseFirestore = mockk(relaxed = true)
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
    fun `toggleFavorite actualiza isFavorite`() = runTest(testDispatcher) {
        val movie = Movie(
            id = 200,
            title = "Peli",
            year = "2023",
            genre = "Acción",
            rating = 4.2,
            reviews = 10,
            description = "Desc",
            director = "Dir",
            duration = "100m",
            imageUrl = null,
            isFavorite = false
        )
        coEvery { movieRepository.getMovieById(200) } returns movie
        coEvery { reviewRepository.getReviewsByMovie(200) } returns Result.success(emptyList())
    
        val vm = MovieDetailViewModel(reviewRepository, movieRepository, authRepository, firestore)
        vm.loadMovieDetail(200)
        // Avanza la ejecución de coroutines de prueba hasta que no haya más tareas pendientes
        advanceUntilIdle()
    
        val state = vm.uiState.value
        assertThat(state.movie?.isFavorite).isFalse()
    }

    @Test
    fun `carga detalle de película y estado inicial`() = runTest(testDispatcher) {
        val movie = Movie(
            id = 200,
            title = "Peli",
            year = "2023",
            genre = "Acción",
            rating = 4.2,
            reviews = 10,
            description = "Desc",
            director = "Dir",
            duration = "100m",
            imageUrl = null,
            isFavorite = false
        )
        coEvery { movieRepository.getMovieById(200) } returns movie
        coEvery { reviewRepository.getReviewsByMovie(200) } returns Result.success(emptyList())
    
        val vm = MovieDetailViewModel(reviewRepository, movieRepository, authRepository, firestore)
        vm.loadMovieDetail(200)
        // Avanza la ejecución de coroutines de prueba hasta que no haya más tareas pendientes
        advanceUntilIdle()
    
        val state = vm.uiState.value
        assertThat(state.movie?.id).isEqualTo(200)
        assertThat(state.reviews).isEmpty()
        assertThat(state.isLoading).isFalse()
    }
}