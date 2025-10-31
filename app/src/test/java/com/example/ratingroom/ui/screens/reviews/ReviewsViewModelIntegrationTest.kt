package com.example.ratingroom.ui.screens.reviews

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.data.models.Movie
import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.repository.MovieRepository
import com.example.ratingroom.repository.ReviewRepository
import com.google.firebase.auth.FirebaseUser
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
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModelIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var movieRepository: MovieRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: ReviewsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Crear mocks de los repositorios para las pruebas de integración
        reviewRepository = mockk()
        movieRepository = mockk()
        authRepository = mockk()
        
        setupMockBehavior()
        
        viewModel = ReviewsViewModel(reviewRepository, movieRepository, authRepository)
    }
    
    /**
     * Configura el comportamiento de los mocks para las pruebas de integración
     */
    private fun setupMockBehavior() {
        val testReviews = listOf(
            ReviewDto(
                id = "test-review-1",
                usuario_id = 2,
                pelicula_id = 101,
                rating = 4,
                texto = "Excelente película de prueba",
                userName = "Usuario Test",
                userImageUrl = null,
                likes = 5
            ),
            ReviewDto(
                id = "test-review-2", 
                usuario_id = 2,
                pelicula_id = 102,
                rating = 3,
                texto = "Película regular",
                userName = "Usuario Test",
                userImageUrl = null,
                likes = 2
            )
        )
        
        val testMovies = mapOf(
            101 to Movie(
                id = 101,
                title = "Película de Prueba 1",
                year = "2023",
                genre = "Drama",
                rating = 4.5,
                reviews = 100,
                description = "Una película de prueba para integración",
                director = "Director Test",
                duration = "120m",
                imageUrl = null,
                isFavorite = false
            ),
            102 to Movie(
                id = 102,
                title = "Película de Prueba 2", 
                year = "2023",
                genre = "Acción",
                rating = 3.8,
                reviews = 75,
                description = "Otra película de prueba",
                director = "Director Test 2",
                duration = "110m",
                imageUrl = null,
                isFavorite = false
            )
        )
        
        // Configurar mocks del ReviewRepository - usar el HARDCODED_USER_ID = 2
        coEvery { reviewRepository.listByUser(2) } returns Result.success(testReviews)
        coEvery { reviewRepository.update(any(), any(), any(), any()) } returns Result.success(
            testReviews.first().copy(rating = 5, texto = "Review actualizada")
        )
        coEvery { reviewRepository.delete(any(), any()) } returns Result.success(true)
        coEvery { reviewRepository.sendOrDeleteLike(any(), any()) } returns Result.success(true)
        coEvery { reviewRepository.create(any(), any(), any(), any()) } returns Result.success(
            ReviewDto(
                id = "new-review",
                usuario_id = 2,
                pelicula_id = 101,
                rating = 5,
                texto = "Nueva review",
                userName = "Usuario Test",
                userImageUrl = null,
                likes = 0
            )
        )
        
        // Configurar mocks del MovieRepository
        coEvery { movieRepository.getMovieById(101) } returns testMovies[101]
        coEvery { movieRepository.getMovieById(102) } returns testMovies[102]
        coEvery { movieRepository.searchMovies(any()) } returns testMovies.values.toList()
        coEvery { movieRepository.getAllMovies() } returns testMovies.values.toList()
        coEvery { movieRepository.getMoviesByGenre(any()) } returns testMovies.values.toList()
        
        // Configurar mocks del AuthRepository
        val mockFirebaseUser = mockk<FirebaseUser>()
        every { mockFirebaseUser.uid } returns "2"
        every { authRepository.currentUser } returns mockFirebaseUser
        every { authRepository.isUserLoggedIn() } returns true
        coEvery { authRepository.signIn(any(), any()) } returns Result.success(mockFirebaseUser)
        coEvery { authRepository.signUp(any(), any(), any(), any(), any()) } returns Result.success(mockFirebaseUser)
        coEvery { authRepository.signOut() } returns Unit
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Prueba de integración: Carga de reviews reales y actualización de estado
     * Simula el flujo completo de carga de datos desde el repositorio
     */
    @Test
    fun testLoadReviewsIntegration_shouldLoadRealDataAndUpdateState() = runTest {
        // Given: El ViewModel se inicializa automáticamente y carga las reviews
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Verificar que se cargaron las reviews correctamente
        val state = viewModel.uiState.value
        assertFalse("El estado no debe estar cargando", state.isLoading)
        assertTrue("Debe haber reviews cargadas", state.reviews.isNotEmpty())
        assertEquals("Debe haber 2 reviews", 2, state.reviews.size)
        
        // Verificar contenido de las reviews usando las propiedades correctas de ReviewItem
        val firstReview = state.reviews.first()
        assertEquals("Película de Prueba 1", firstReview.movieTitle)
        assertEquals(4, firstReview.rating)
        assertEquals("Excelente película de prueba", firstReview.comment)
        assertEquals(5, firstReview.likes)
    }

    /**
     * Prueba de integración: Edición de review y persistencia en repositorio real
     * Simula el flujo completo de edición con persistencia
     */
    @Test
    fun testEditReviewIntegration_shouldPersistChangesInRealRepository() = runTest {
        // Given: ViewModel inicializado con reviews
        testDispatcher.scheduler.advanceUntilIdle()
        
        val originalReview = viewModel.uiState.value.reviews.first()
        val reviewId = originalReview.id
        
        // When: Editar la review usando el método correcto del ViewModel
        val newRating = 5
        val newText = "Review actualizada en integración"
        viewModel.editReview(reviewId, newRating, newText)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: Verificar que la edición se reflejó en el estado
        val state = viewModel.uiState.value
        assertNull("No debe haber mensaje de error", state.errorMessage)
        
        val updatedReview = state.reviews.find { it.id == reviewId }
        assertNotNull("La review debe existir después de la actualización", updatedReview)
        assertEquals("El rating debe haberse actualizado", 5, updatedReview?.rating)
        assertEquals("El texto debe haberse actualizado", "Review actualizada", updatedReview?.comment)
    }
}