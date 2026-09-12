package com.easytrain.feature.onboarding.profilesetup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import com.easytrain.core.model.Units
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileSetupScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `saving is blocked until a name is entered`() {
        composeTestRule.setContent {
            EasyTrainTheme {
                ProfileSetupScreen(
                    state = ProfileSetupUiState(),
                    onDisplayNameChanged = {},
                    onUnitsChanged = {},
                    onSubmit = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun `picking pounds reports the unit change`() {
        var units: Units? = null
        composeTestRule.setContent {
            EasyTrainTheme {
                ProfileSetupScreen(
                    state = ProfileSetupUiState(displayName = "Dana"),
                    onDisplayNameChanged = {},
                    onUnitsChanged = { units = it },
                    onSubmit = {},
                )
            }
        }

        composeTestRule.onNodeWithText("lb").assertIsDisplayed().performClick()

        assertEquals(Units.LB, units)
    }
}
