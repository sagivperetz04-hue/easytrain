package com.easytrain.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthPlaceholderScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the placeholder screen renders the app name`() {
        composeTestRule.setContent {
            EasyTrainTheme {
                AuthPlaceholderScreen()
            }
        }

        composeTestRule.onNodeWithText("EasyTrain").assertIsDisplayed()
    }
}
