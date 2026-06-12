package com.kidzone.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidzone.presentation.auth.LoginScreen
import com.kidzone.ui.theme.KidZoneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Login screen.
 *
 * Tests cover:
 * - Initial state (empty fields, button disabled)
 * - Email/password input
 * - Validation feedback
 * - Navigation to register screen
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysAllElements() {
        composeTestRule.setContent {
            KidZoneTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToRegister = {}
                )
            }
        }

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
        composeTestRule.setContent {
            KidZoneTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Login button should be disabled with empty fields
        composeTestRule.onNodeWithText("Zaloguj").assertIsNotEnabled()
    }

    @Test
    fun loginButton_enabledWhenFieldsFilled() {
        composeTestRule.setContent {
            KidZoneTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Fill email
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        // Fill password
        composeTestRule.onNodeWithText("Hasło").performTextInput("password123")

        // Login button should be enabled
        composeTestRule.onNodeWithText("Zaloguj").assertIsEnabled()
    }

    @Test
    fun registerLink_isClickable() {
        var navigatedToRegister = false

        composeTestRule.setContent {
            KidZoneTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToRegister = { navigatedToRegister = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Nie masz konta? Zarejestruj się").performClick()
        assert(navigatedToRegister)
    }

    @Test
    fun forgotPasswordLink_isDisplayed() {
        composeTestRule.setContent {
            KidZoneTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToRegister = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Zapomniałeś hasła?").assertIsDisplayed()
    }
}
