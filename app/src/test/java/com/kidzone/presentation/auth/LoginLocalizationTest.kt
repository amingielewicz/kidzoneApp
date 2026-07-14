package com.kidzone.presentation.auth

import com.kidzone.R
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.utils.AuthException
import com.kidzone.utils.UiText
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginLocalizationTest {

    private val viewModel = LoginViewModel(mockk<AuthRepository>(relaxed = true))

    @Test
    fun `technical Google sign-in error is replaced with localized message`() {
        viewModel.showInlineMessage("Google sign-in error (code: 10)")

        val message = viewModel.uiState.value.message
        assertTrue(message is UiText.StringResource)
        assertEquals(
            R.string.google_sign_in_unavailable,
            (message as UiText.StringResource).resId
        )
    }

    @Test
    fun `ordinary dynamic message remains unchanged`() {
        viewModel.showInlineMessage("Test error")

        val message = viewModel.uiState.value.message
        assertTrue(message is UiText.DynamicString)
        assertEquals("Test error", (message as UiText.DynamicString).value)
    }

    @Test
    fun `account ban exposes localized fallback resource`() {
        val error = AuthException.AccountBanned(
            banMessage = "Konto zablokowane",
            banReason = "Spam"
        )

        assertEquals(R.string.error_account_banned, error.messageRes)
    }
}
