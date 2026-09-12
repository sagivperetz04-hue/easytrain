package com.easytrain.feature.onboarding.invite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnterInviteCodeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the stub says what is coming and can be skipped`() {
        var skipped = false
        composeTestRule.setContent {
            EasyTrainTheme { EnterInviteCodeScreen(onSkip = { skipped = true }) }
        }

        composeTestRule.onNodeWithText("Entering an invite code arrives in ET-003.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip for now").performClick()

        assertTrue(skipped)
    }
}
