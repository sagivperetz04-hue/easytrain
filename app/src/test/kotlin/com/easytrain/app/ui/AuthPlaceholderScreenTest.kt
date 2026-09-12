package com.easytrain.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric sandboxes for SDK 36+ require Java 21; the project standard is JDK 17.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
