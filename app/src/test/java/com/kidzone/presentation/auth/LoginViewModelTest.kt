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
 * - Weryfikacja przepływu uwierzytelniania w [LoginViewModel].
 * - Sprawdzenie poprawnego mapowania błędów logowania na komunikaty UI.
 *
 * 🛠️ Środowisko:
 * - Mockowanie [AuthRepository] dla izolacji od Firebase.
 * - [MainDispatcherRule] do synchronizacji coroutines.
 *
 * 🔍 Scenariusze:
 * - Sukces logowania (email/hasło).
 * - Obsługa błędnych poświadczeń i niezweryfikowanego adresu email.
 * - Wykrywanie i komunikowanie blokad konta (Bans).
 * - Obsługa resetowania hasła i ponownej wysyłki weryfikacji.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        authRepository = mockk(relaxed = true)
        viewModel = LoginViewModel(authRepository)
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
            assertEquals("", state.email)
            assertEquals("", state.password)
            assertFalse(state.isFormValid)
            assertFalse(state.isLoading)
            assertFalse(state.isSignedIn)
            assertNull(state.message)
        }

        @Test
        fun `form is invalid when email is empty`() {
            viewModel.onPasswordChange("password123")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `form is invalid when password is empty`() {
            viewModel.onEmailChange("user@test.com")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `form is valid when both fields are filled`() {
            viewModel.onEmailChange("user@test.com")
            viewModel.onPasswordChange("password123")
            assertTrue(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `form is invalid when email is only whitespace`() {
            viewModel.onEmailChange("   ")
            viewModel.onPasswordChange("password123")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

        @Test
        fun `changing email clears previous message`() {
            viewModel.showInlineMessage("Error occurred")
            assertNotNull(viewModel.uiState.value.message)

            viewModel.onEmailChange("new@email.com")
            assertNull(viewModel.uiState.value.message)
        }

        @Test
        fun `changing password clears previous message`() {
            viewModel.showInlineMessage("Error occurred")
            viewModel.onPasswordChange("newpass")
            assertNull(viewModel.uiState.value.message)
        }
    }

    // =========================================================================
    // Sign in with email
    // =========================================================================

    @Nested
    @DisplayName("signIn()")
    inner class SignIn {

        @Test
        fun `shows error when email is blank`() = runTest {
            viewModel.onPasswordChange("password123")
            viewModel.signIn()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.message is UiText.StringResource)
            assertEquals(R.string.login_validation_empty, (state.message as UiText.StringResource).resId)
            assertTrue(state.isMessageError)
            assertFalse(state.isSignedIn)
        }

        @Test
        fun `shows error when password is blank`() = runTest {
            viewModel.onEmailChange("user@test.com")
            viewModel.signIn()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.message is UiText.StringResource)
            assertEquals(R.string.login_validation_empty, (state.message as UiText.StringResource).resId)
            assertTrue(state.isMessageError)
        }

        @Test
        fun `successful sign in sets isSignedIn to true`() = runTest {
            val user = TestFixtures.user()
            coEvery { authRepository.signInWithEmail(any(), any()) } returns OpResult.success(user)

            viewModel.onEmailChange("user@test.com")
            viewModel.onPasswordChange("password123")
            viewModel.signIn()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isSignedIn)
            assertFalse(state.isLoading)
            assertNull(state.message)
        }

        @Test
        fun `trims email before sending to repository`() = runTest {
            val user = TestFixtures.user()
            coEvery { authRepository.signInWithEmail(any(), any()) } returns OpResult.success(user)

            viewModel.onEmailChange("  user@test.com  ")
            viewModel.onPasswordChange("password123")
            viewModel.signIn()
            advanceUntilIdle()

            coVerify { authRepository.signInWithEmail("user@test.com", "password123") }
        }

        @Test
        fun `failed sign in shows error message`() = runTest {
            coEvery { authRepository.signInWithEmail(any(), any()) } returns
                OpResult.failure(AuthException.InvalidCredentials)

            viewModel.onEmailChange("user@test.com")
            viewModel.onPasswordChange("wrongpass")
            viewModel.signIn()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSignedIn)
            assertFalse(state.isLoading)
            assertTrue(state.isMessageError)
            assertNotNull(state.message)
        }

        @Test
        fun `email not verified shows resend verification button`() = runTest {
            coEvery { authRepository.signInWithEmail(any(), any()) } returns
                OpResult.failure(AuthException.EmailNotVerified)

            viewModel.onEmailChange("user@test.com")
            viewModel.onPasswordChange("password123")
            viewModel.signIn()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.showResendVerification)
            assertFalse(state.isSignedIn)
        }

        @Test
        fun `account banned sets ban message and reason`() = runTest {
            coEvery { authRepository.signInWithEmail(any(), any()) } returns
                OpResult.failure(AuthException.AccountBanned("Konto zablokowane", "Spam"))

            viewModel.onEmailChange("user@test.com")
            viewModel.onPasswordChange("password123")
            viewModel.signIn()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Konto zablokowane", state.banMessage)
            assertEquals("Spam", state.banReason)
            assertFalse(state.isSignedIn)
        }

        @Test
        fun `isLoading is true during sign in request`() = runTest {
            coEvery { authRepository.signInWithEmail(any(), any()) } coAnswers {
                // Verify loading state DURING the call
                assertTrue(viewModel.uiState.value.isLoading)
                OpResult.success(TestFixtures.user())
            }

            viewModel.onEmailChange("user@test.com")
            viewModel.onPasswordChange("password123")
            viewModel.signIn()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
        }
    }

    // =========================================================================
    // Sign in with Google
    // =========================================================================

    @Nested
    @DisplayName("signInWithGoogle()")
    inner class SignInWithGoogle {

        @Test
        fun `successful Google sign in sets isSignedIn to true`() = runTest {
            coEvery { authRepository.signInWithGoogle(any()) } returns
                OpResult.success(TestFixtures.user())

            viewModel.signInWithGoogle("fake-id-token")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isSignedIn)
            assertFalse(state.isLoading)
        }

        @Test
        fun `failed Google sign in shows error message`() = runTest {
            coEvery { authRepository.signInWithGoogle(any()) } returns
                OpResult.failure(AuthException.Network(RuntimeException("timeout")))

            viewModel.signInWithGoogle("fake-id-token")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSignedIn)
            assertTrue(state.isMessageError)
            assertNotNull(state.message)
        }

        @Test
        fun `banned account via Google shows ban info`() = runTest {
            coEvery { authRepository.signInWithGoogle(any()) } returns
                OpResult.failure(AuthException.AccountBanned("Zablokowane", "Naruszenie"))

            viewModel.signInWithGoogle("fake-token")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Zablokowane", state.banMessage)
            assertEquals("Naruszenie", state.banReason)
        }
    }

    // =========================================================================
    // Forgot password
    // =========================================================================

    @Nested
    @DisplayName("forgotPassword()")
    inner class ForgotPassword {

        @Test
        fun `shows error when email field is empty`() = runTest {
            viewModel.forgotPassword()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isMessageError)
            assertTrue(state.message is UiText.StringResource)
            assertEquals(R.string.forgot_password_hint, (state.message as UiText.StringResource).resId)
        }

        @Test
        fun `successful password reset shows info message`() = runTest {
            coEvery { authRepository.sendPasswordResetEmail(any()) } returns OpResult.success(Unit)

            viewModel.onEmailChange("user@test.com")
            viewModel.forgotPassword()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isMessageError)
            assertTrue(state.message is UiText.StringResource)
            assertEquals(R.string.password_reset_sent, (state.message as UiText.StringResource).resId)
            assertEquals("user@test.com", (state.message as UiText.StringResource).args[0])
            assertFalse(state.isLoading)
        }

        @Test
        fun `failed password reset shows error`() = runTest {
            coEvery { authRepository.sendPasswordResetEmail(any()) } returns
                OpResult.failure(AuthException.UserNotFound)

            viewModel.onEmailChange("nonexistent@test.com")
            viewModel.forgotPassword()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isMessageError)
            assertNotNull(state.message)
        }

        @Test
        fun `trims email before sending reset`() = runTest {
            coEvery { authRepository.sendPasswordResetEmail(any()) } returns OpResult.success(Unit)

            viewModel.onEmailChange("  user@test.com  ")
            viewModel.forgotPassword()
            advanceUntilIdle()

            coVerify { authRepository.sendPasswordResetEmail("user@test.com") }
        }
    }

    // =========================================================================
    // Resend verification email
    // =========================================================================

    @Nested
    @DisplayName("resendVerificationEmail()")
    inner class ResendVerification {

        @Test
        fun `does nothing when email is blank`() = runTest {
            viewModel.onPasswordChange("password")
            viewModel.resendVerificationEmail()
            advanceUntilIdle()

            coVerify(exactly = 0) { authRepository.resendVerificationEmail(any(), any()) }
        }

        @Test
        fun `does nothing when password is blank`() = runTest {
            viewModel.onEmailChange("user@test.com")
            viewModel.resendVerificationEmail()
            advanceUntilIdle()

            coVerify(exactly = 0) { authRepository.resendVerificationEmail(any(), any()) }
        }

        @Test
        fun `successful resend shows info message and hides button`() = runTest {
            coEvery { authRepository.resendVerificationEmail(any(), any()) } returns
                OpResult.success(Unit)

            viewModel.onEmailChange("user@test.com")
            viewModel.onPasswordChange("password123")
            viewModel.resendVerificationEmail()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isMessageError)
            assertFalse(state.showResendVerification)
            assertTrue(state.message is UiText.StringResource)
            assertEquals(R.string.verification_email_sent, (state.message as UiText.StringResource).resId)
        }

        @Test
        fun `failed resend shows error message`() = runTest {
            coEvery { authRepository.resendVerificationEmail(any(), any()) } returns
                OpResult.failure(RuntimeException("Quota exceeded"))

            viewModel.onEmailChange("user@test.com")
            viewModel.onPasswordChange("password123")
            viewModel.resendVerificationEmail()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isMessageError)
            assertTrue(state.message is UiText.StringResource)
            assertEquals(R.string.verification_email_error, (state.message as UiText.StringResource).resId)
        }
    }

    // =========================================================================
    // showInlineMessage
    // =========================================================================

    @Test
    fun `showInlineMessage sets message and isError flag`() {
        viewModel.showInlineMessage("Test error", isError = true)
        assertTrue(viewModel.uiState.value.message is UiText.DynamicString)
        assertEquals("Test error", (viewModel.uiState.value.message as UiText.DynamicString).value)
        assertTrue(viewModel.uiState.value.isMessageError)

        viewModel.showInlineMessage("Test info", isError = false)
        assertTrue(viewModel.uiState.value.message is UiText.DynamicString)
        assertEquals("Test info", (viewModel.uiState.value.message as UiText.DynamicString).value)
        assertFalse(viewModel.uiState.value.isMessageError)
    }
}
