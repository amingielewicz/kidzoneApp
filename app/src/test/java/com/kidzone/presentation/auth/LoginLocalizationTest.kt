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
    fun `error message is correctly set in state`() {
        val error = UiText.StringResource(R.string.google_sign_in_unavailable)
        viewModel.showErrorMessage(error)

        val message = viewModel.uiState.value.message
        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.google_sign_in_unavailable, (message as UiText.StringResource).resId)
        assertTrue(viewModel.uiState.value.isMessageError)
    }

    @Test
    fun `info message is correctly set in state`() {
        val info = UiText.DynamicString("Info message")
        viewModel.showInfoMessage(info)

        val message = viewModel.uiState.value.message
        assertTrue(message is UiText.DynamicString)
        assertEquals("Info message", (message as UiText.DynamicString).value)
        assertTrue(!viewModel.uiState.value.isMessageError)
    }

    @Test
    fun `account ban exposes localized fallback resource`() {
        val error = AuthException.AccountBanned(
            resId = R.string.error_account_banned,
            banReasonRes = R.string.ban_reason_spam
        )

        assertEquals(R.string.error_account_banned, error.messageRes)
    }
}
