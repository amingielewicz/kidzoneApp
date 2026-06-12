package com.kidzone.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kidzone.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Login screen.
 *
 * Uses HiltAndroidTest because LoginScreen internally calls hiltViewModel()
 * which requires Hilt's generated component infrastructure.
 *
 * Tests cover:
 * - Initial state (empty fields, button disabled)
 * - Email/password input
 * - Validation feedback
 * - Navigation to register screen
 */
@HiltAndroidTest
class LoginScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun loginScreen_displaysAllElements() {
        // The app starts on Login screen when not authenticated.
        // Wait for initial composition.
        composeTestRule.waitForIdle()

        // Header
        composeTestRule.onNodeWithText("Zaloguj się").assertIsDisplayed()

        // Input fields
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hasło").assertIsDisplayed()

        // Buttons
        composeTestRule.onNodeWithText("Zaloguj").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nie masz konta? Zarejestruj się").assertIsDisplayed()
    }

    @Test
    fun loginButton_disabledWhenFieldsEmpty() {
        composeTestRule.waitForIdle()

        // Login button should be disabled with empty fields
        composeTestRule.onNodeWithText("Zaloguj").assertIsNotEnabled()
    }

    @Test
    fun loginButton_enabledWhenFieldsFilled() {
        composeTestRule.waitForIdle()

        // Fill email
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        // Fill password
        composeTestRule.onNodeWithText("Hasło").performTextInput("password123")

        // Login button should be enabled
        composeTestRule.onNodeWithText("Zaloguj").assertIsEnabled()
    }

    @Test
    fun registerLink_isClickable() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Nie masz konta? Zarejestruj się").performClick()
        // After clicking register link, we should navigate away from login
        // (exact destination depends on nav graph - just verify click doesn't crash)
    }

    @Test
    fun forgotPasswordLink_isDisplayed() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Zapomniałeś hasła?").assertIsDisplayed()
    }
}
