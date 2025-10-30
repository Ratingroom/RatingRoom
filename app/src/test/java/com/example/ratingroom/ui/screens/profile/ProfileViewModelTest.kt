package com.example.ratingroom.ui.screens.profile

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
class ProfileViewModelTest {

    private val authRepository: com.example.ratingroom.repository.AuthRepository = mockk(relaxed = true)
    private val reviewRepository: com.example.ratingroom.repository.ReviewRepository = mockk(relaxed = true)
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
    fun `carga perfil con favoritos`() = runTest(testDispatcher) {
        // Simular usuario actual y perfil
        coEvery { authRepository.currentUser } returns mockk(relaxed = true)
        coEvery { authRepository.getUserProfile() } returns Result.success(
            com.example.ratingroom.repository.UserProfile(
                uid = "uid123",
                email = "user@example.com",
                fullName = "Usuario Test",
                favoriteGenre = "Drama",
                createdAt = System.currentTimeMillis()
            )
        )
        coEvery { reviewRepository.listByUser(any()) } returns Result.success(emptyList())

        val vm = ProfileViewModel(reviewRepository, authRepository, firestore)
        // Avanza la ejecución de coroutines de prueba hasta que no haya más tareas pendientes
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.profileData?.name).isEqualTo("Usuario Test")
        assertThat(state.isLoading).isFalse()
    }
}