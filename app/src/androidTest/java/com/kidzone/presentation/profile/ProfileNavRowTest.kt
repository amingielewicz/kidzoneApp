package com.kidzone.presentation.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ProfileNavRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileNavRow_exposesButtonRoleAndMinimumTouchTarget() {
        composeRule.setContent {
            MaterialTheme {
                ProfileNavRow(
                    icon = Icons.Filled.Place,
                    label = "Moje miejsca",
                    trailingText = "3",
                    onClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Moje miejsca")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }
}
