package com.example.ratingroom.ui.screens.forgotpassword

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
class ForgotPasswordViewModelTest {

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
    fun `envio email falla por email invalido`() = runTest(testDispatcher) {
        val vm = ForgotPasswordViewModel(authRepository)
        vm.onEmailChange("invalid")

        var successCalled = false
        vm.sendRecoveryEmail { successCalled = true }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Por favor ingresa un email válido")
        assertThat(successCalled).isFalse()
    }

    @Test
    fun `envio email exitoso actualiza successMessage y llama onSuccess`() = runTest(testDispatcher) {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.success(Unit)

        val vm = ForgotPasswordViewModel(authRepository)
        vm.onEmailChange("user@example.com")

        var successCalled = false
        vm.sendRecoveryEmail { successCalled = true }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.successMessage).isEqualTo("Se ha enviado un enlace de recuperación a tu email")
        assertThat(state.errorMessage).isNull()
        assertThat(successCalled).isTrue()
    }
}