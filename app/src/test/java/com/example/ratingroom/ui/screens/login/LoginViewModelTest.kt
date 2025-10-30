package com.example.ratingroom.ui.screens.login

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
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
class LoginViewModelTest {

    private val authRepository: com.example.ratingroom.repository.AuthRepository = mockk(relaxed = true)
    private val fcmTokenManager: com.example.ratingroom.utils.FCMTokenManager = mockk(relaxed = true)
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
    fun `login success actualiza estado y llama refreshFCMToken y onSuccess`() = runTest(testDispatcher) {
        val firebaseUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
        every { firebaseUser.uid } returns "uid-123"
        coEvery { authRepository.signIn(email = any(), password = any()) } returns Result.success(firebaseUser)

        var successCalled = false
        val vm = LoginViewModel(authRepository, fcmTokenManager)

        vm.onUsernameChange("user@example.com")
        vm.onPasswordChange("123456")
        vm.login { successCalled = true }

        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(successCalled).isTrue()
        coVerify { fcmTokenManager.refreshFCMToken() }
    }

    @Test
    fun `login invalida email y muestra error`() = runTest(testDispatcher) {
        var successCalled = false
        val vm = LoginViewModel(authRepository, fcmTokenManager)

        vm.onUsernameChange("invalid")
        vm.onPasswordChange("123456")
        vm.login { successCalled = true }

        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Email inválido")
        assertThat(successCalled).isFalse()
    }
}