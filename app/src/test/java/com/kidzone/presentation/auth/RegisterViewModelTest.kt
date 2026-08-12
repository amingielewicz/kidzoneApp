package com.kidzone.presentation.auth

import com.kidzone.R
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.AuthException
import com.kidzone.utils.OpResult
import com.kidzone.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

/**
 * 🧪 Cel testu:
 * - Weryfikacja procesu rejestracji nowego użytkownika przez [RegisterViewModel].
 * - Sprawdzenie lokalnej walidacji formularza i obsługi błędów z backendu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @BeforeEach
    fun setUp() {
        authRepository = mockk(relaxed = true)
        viewModel = RegisterViewModel(authRepository)
    }

    @Nested
    @DisplayName("Form validation")
    inner class FormValidation {

        @Test
        fun `initial state has empty fields and form is invalid`() {
            val state = viewModel.uiState.value
            assertEquals("", state.name)
            assertEquals("", state.email)
            assertEquals("", state.password)
            assertFalse(state.isTosAccepted)
            assertFalse(state.isFormValid)
        }

        @Test
        fun `isEmailValid returns true for valid email`() {
            viewModel.onEmailChange("user@example.com")
            assertTrue(viewModel.uiState.value.isEmailValid)
        }

        @Test
        fun `isPasswordValid returns true for strong password`() {
            viewModel.onPasswordChange("StrongP@ss123!")
            assertTrue(viewModel.uiState.value.isPasswordValid)
        }

        @Test
        fun `isFormValid becomes true when all requirements are met`() {
            viewModel.onNameChange("Jan Kowalski")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss123!")
            viewModel.onTosAcceptanceChange(true)
            
            val state = viewModel.uiState.value
            assertTrue(state.isNameValid, "Name should be valid")
            assertTrue(state.isEmailValid, "Email should be valid")
            assertTrue(state.isPasswordValid, "Password should be valid")
            assertTrue(state.isTosAccepted, "TOS should be accepted")
            assertTrue(state.isFormValid, "Whole form should be valid")
        }

        @Test
        fun `isFormValid remains false when TOS is not accepted`() {
            viewModel.onNameChange("Jan Kowalski")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss123!")
            viewModel.onTosAcceptanceChange(false)
            assertFalse(viewModel.uiState.value.isFormValid)
        }
    }

    @Nested
    @DisplayName("Registration process")
    inner class Register {

        @Test
        fun `successful registration sets isRegistered`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.success(TestFixtures.user())

            viewModel.onNameChange("Jan Kowalski")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss123!")
            viewModel.onTosAcceptanceChange(true)
            
            viewModel.register()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isRegistered)
            coVerify { authRepository.signOut() }
        }

        @Test
        fun `shows error when TOS not accepted during registration`() = runTest {
            viewModel.onNameChange("Jan")
            viewModel.onEmailChange("jan@example.com")
            viewModel.onPasswordChange("StrongP@ss123!")
            viewModel.onTosAcceptanceChange(false)
            
            viewModel.register()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(R.string.field_required, (state.errorMessage as? UiText.StringResource)?.resId)
        }
        
        @Test
        fun `trims inputs before sending to repository`() = runTest {
            coEvery { authRepository.registerWithEmail(any(), any(), any()) } returns
                OpResult.success(TestFixtures.user())

            viewModel.onNameChange("  Jan  ")
            viewModel.onEmailChange("  jan@example.com  ")
            viewModel.onPasswordChange("StrongP@ss123!")
            viewModel.onTosAcceptanceChange(true)
            
            viewModel.register()
            advanceUntilIdle()

            coVerify { authRepository.registerWithEmail("Jan", "jan@example.com", "StrongP@ss123!") }
        }
    }
}
