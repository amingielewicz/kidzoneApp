package com.kidzone.presentation.auth

import com.kidzone.domain.repository.AuthRepository
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.AuthException
import com.kidzone.utils.OpResult
import com.kidzone.utils.PasswordPolicy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @BeforeEach
    fun setUp() {
        authRepository = mockk(relaxed = true)
        viewModel = RegisterViewModel(authRepository)
    }

    // =========================================================================
    // Form validation
    // =========================================================================

    @Nested
    @DisplayName("Form validation")
    inner class FormValidation {

        @Test
        fun `initial state has empty fields and form is invalid`() {
            val state = viewModel.uiState.value
            assertEquals("", state.name)
            assertEquals("", state.email)
            assertEquals("", state.password)
            assertFalse(state.isFormValid)
            assertFalse(state.isLoading)
            assertFalse(state.isRegistered)
        }

        @Test
        fun `isNameValid returns false for blank name`() {
            viewModel.onNameChange("   ")
            assertFalse(viewModel.uiState.value.isNameValid)
        }

        @Test
        fun `isNameValid returns true for non-blank name`() {
            viewModel.onNameChange("Jan")
            assertTrue(viewModel.uiState.value.isNameValid)
        }

        @Test
        fun `isEmailValid returns false for missing at sign`() {
            viewModel.onEmailChange("invalid-email")
            assertFalse(viewModel.uiState.value.isEmailValid)
        }

        @Test
        fun `isEmailValid returns false for missing domain dot`() {
            viewModel.onEmailChange("user@nodot")
            assertFalse(viewModel.uiState.value.isEmailValid)
        }

        @Test
        fun `isEmailValid returns false for too short email`() {
            viewModel.onEmailChange("a@b.")
            assertFalse(viewModel.uiState.value.isEmailValid)
        }

        @Test
        fun `isEmailValid returns true for valid email`() {
            viewModel.onEmailChange("user@example.com")
            assertTrue(viewModel.uiState.value.isEmailValid)
        }

        @Test
        fun `isPasswordValid returns false for weak password`() {
            viewModel.onPasswordChange("short")
            assertFalse(viewModel.uiState.value.isPasswordValid)
        }

        @Test
        fun `isPasswordValid returns true for strong password`() {
            viewModel.onPasswordChange("StrongP@ss1")
            assertTrue(viewModel.uiState.value.isPasswordValid)
        }

        @Test
        fun `isFormValid is true only when all fields valid`() {
            viewModel.onNameChange("Jan Kowalski")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            assertTrue(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `isFormValid is false when password is weak`() {
            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("weak")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `changing name clears error message`() {
            // Force an error first
            viewModel.onNameChange("")
            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            // Trigger register to set errorMessage for blank name
            runTest {
                viewModel.register()
                advanceUntilIdle()
            }
            assertNotNull(viewModel.uiState.value.errorMessage)

            viewModel.onNameChange("Jan")
            assertNull(viewModel.uiState.value.errorMessage)
        }
    }

    // =========================================================================
    // Registration
    // =========================================================================

    @Nested
    @DisplayName("register()")
    inner class Register {

        @Test
        fun `shows error when name is blank`() = runTest {
            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            assertEquals("Podaj imię / nazwę użytkownika", viewModel.uiState.value.errorMessage)
        }

        @Test
        fun `shows error when email is blank`() = runTest {
            viewModel.onNameChange("Jan")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            assertEquals("Podaj e-mail", viewModel.uiState.value.errorMessage)
        }

        @Test
        fun `shows error when password is blank`() = runTest {
            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("user@example.com")
            viewModel.register()
            advanceUntilIdle()

            assertEquals("Podaj hasło", viewModel.uiState.value.errorMessage)
        }

        @Test
        fun `shows password policy error for weak password`() = runTest {
            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("weak")
            viewModel.register()
            advanceUntilIdle()

            assertEquals(PasswordPolicy.DEFAULT_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)
        }

        @Test
        fun `successful registration sets isRegistered and shows success message`() = runTest {
            val user = TestFixtures.user()
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.success(user)

            viewModel.onNameChange("Jan Kowalski")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isRegistered)
            assertNotNull(state.successMessage)
            assertTrue(state.successMessage!!.contains("Konto utworzone"))
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }

        @Test
        fun `signs out after successful registration`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.success(TestFixtures.user())

            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            coVerify { authRepository.signOut() }
        }

        @Test
        fun `does not sign out on registration failure`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.failure(AuthException.EmailAlreadyInUse)

            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            coVerify(exactly = 0) { authRepository.signOut() }
        }

        @Test
        fun `trims name and email before sending`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.success(TestFixtures.user())

            viewModel.onNameChange("  Jan  ")
            viewModel.onEmailChange("  jan@example.com  ")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            coVerify { authRepository.registerWithEmail("Jan", "jan@example.com", "StrongP@ss1") }
        }

        @Test
        fun `email already in use shows appropriate error`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.failure(AuthException.EmailAlreadyInUse)

            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("existing@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRegistered)
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage!!.contains("istnieje"))
        }

        @Test
        fun `username already taken shows appropriate error`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.failure(AuthException.UsernameAlreadyTaken)

            viewModel.onNameChange("ExistingUser")
            viewModel.onEmailChange("new@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRegistered)
            assertTrue(state.errorMessage!!.contains("zajęta"))
        }

        @Test
        fun `isLoading is true during registration`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } coAnswers {
                assertTrue(viewModel.uiState.value.isLoading)
                OpResult.success(TestFixtures.user())
            }

            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
        }

        @Test
        fun `network error shows generic error message`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.failure(AuthException.Network(RuntimeException("Connection timeout")))

            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss1")
            viewModel.register()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Błąd połączenia z serwerem. Spróbuj ponownie.", state.errorMessage)
            assertFalse(state.isRegistered)
        }
    }
}
