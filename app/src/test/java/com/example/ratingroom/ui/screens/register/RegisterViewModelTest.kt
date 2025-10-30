package com.example.ratingroom.ui.screens.register

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
class RegisterViewModelTest {

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
    fun `register falla por contraseñas no coinciden`() = runTest(testDispatcher) {
        val vm = RegisterViewModel(authRepository)
        vm.onFullNameChange("John Doe")
        vm.onEmailChange("john@example.com")
        vm.onPasswordChange("123456")
        vm.onConfirmPasswordChange("654321")

        vm.register()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Las contraseñas no coinciden")
    }

    @Test
    fun `register exitoso actualiza successMessage y llama onSuccess`() = runTest(testDispatcher) {
        val firebaseUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
        every { firebaseUser.uid } returns "uid-abc"
        coEvery { authRepository.signUp(email = any(), password = any(), displayName = any(), favoriteGenre = any(), birthYear = any()) } returns Result.success(firebaseUser)

        var onSuccessCalled = false
        val vm = RegisterViewModel(authRepository)
        vm.onFullNameChange("Jane Roe")
        vm.onEmailChange("jane@example.com")
        vm.onPasswordChange("123456")
        vm.onConfirmPasswordChange("123456")

        vm.register { onSuccessCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.successMessage).isEqualTo("Registro exitoso")
        assertThat(state.errorMessage).isNull()
        assertThat(onSuccessCalled).isTrue()
    }
}