package com.kidzone.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidzone.ui.theme.KidZoneTheme
import com.kidzone.presentation.maintenance.MaintenanceScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for Place Details and Maintenance screens.
 *
 * Note: Full PlaceDetailsScreen test requires Hilt injection (ViewModel).
 * These tests cover standalone composables that can render without DI.
 */
@RunWith(AndroidJUnit4::class)
class PlaceDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun maintenanceScreen_displaysMessageCorrectly() {
        val testMessage = "Serwer jest w trakcie aktualizacji. Wróć za 30 minut."

        composeTestRule.setContent {
            KidZoneTheme {
                MaintenanceScreen(message = testMessage)
            }
        }

        composeTestRule.onNodeWithText("Przerwa techniczna").assertIsDisplayed()
        composeTestRule.onNodeWithText(testMessage).assertIsDisplayed()
    }

    @Test
    fun maintenanceScreen_displaysDefaultMessage() {
        val defaultMsg = "Aplikacja jest chwilowo niedostępna. Spróbuj ponownie później."

        composeTestRule.setContent {
            KidZoneTheme {
                MaintenanceScreen(message = defaultMsg)
            }
        }

        composeTestRule.onNodeWithText("Przerwa techniczna").assertIsDisplayed()
        composeTestRule.onNodeWithText(defaultMsg).assertIsDisplayed()
    }
}
