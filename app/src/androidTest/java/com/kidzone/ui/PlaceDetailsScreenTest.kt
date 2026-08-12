package com.kidzone.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidzone.R
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

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun maintenanceScreen_displaysMessageCorrectly() {
        val testMessage = "Serwer jest w trakcie aktualizacji. Wróć za 30 minut."
        val title = context.getString(R.string.maintenance_title)

        composeTestRule.setContent {
            KidZoneTheme {
                MaintenanceScreen(message = testMessage)
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(testMessage).assertIsDisplayed()
    }

    @Test
    fun maintenanceScreen_displaysDefaultMessage() {
        val defaultMsg = context.getString(R.string.maintenance_message)
        val title = context.getString(R.string.maintenance_title)

        composeTestRule.setContent {
            KidZoneTheme {
                MaintenanceScreen(message = defaultMsg)
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(defaultMsg).assertIsDisplayed()
    }
}
