package com.easytrain.feature.onboarding.chooserole

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import com.easytrain.core.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChooseRoleScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `both roles are offered and the choice is reported once`() {
        var chosen: UserRole? = null
        composeTestRule.setContent {
            EasyTrainTheme {
                ChooseRoleScreen(state = ChooseRoleUiState(), onRoleChosen = { chosen = it })
            }
        }

        composeTestRule.onNodeWithText("I'm a coach").assertIsDisplayed()
        composeTestRule.onNodeWithText("I'm a trainee").assertIsDisplayed()

        composeTestRule.onNodeWithText("I'm a trainee").performClick()

        assertEquals(UserRole.TRAINEE, chosen)
    }

    @Test
    fun `the permanence of the choice is spelled out`() {
        composeTestRule.setContent {
            EasyTrainTheme {
                ChooseRoleScreen(state = ChooseRoleUiState(), onRoleChosen = {})
            }
        }

        composeTestRule
            .onNodeWithText("This is permanent — a second account is needed to switch.")
            .assertIsDisplayed()
    }
}
