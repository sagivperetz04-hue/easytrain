package com.easytrain.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import com.easytrain.core.model.UserRole
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric sandboxes for SDK 36+ require Java 21; the project standard is JDK 17.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomePlaceholderScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the coach lands on their trainees and can sign out`() {
        var signedOut = false
        composeTestRule.setContent {
            EasyTrainTheme {
                HomePlaceholderScreen(role = UserRole.COACH, onSignOut = { signedOut = true })
            }
        }

        composeTestRule.onNodeWithText("Your trainees").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign out").performClick()

        assertTrue(signedOut)
    }

    @Test
    fun `the trainee lands on today`() {
        composeTestRule.setContent {
            EasyTrainTheme {
                HomePlaceholderScreen(role = UserRole.TRAINEE, onSignOut = {})
            }
        }

        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    }
}
