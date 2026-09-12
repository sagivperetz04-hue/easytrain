package com.easytrain.feature.onboarding.forgotpassword

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForgotPasswordScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `once sent, the screen says where to look`() {
        composeTestRule.setContent {
            EasyTrainTheme {
                ForgotPasswordScreen(
                    state = ForgotPasswordUiState(email = "dana@example.com", isSent = true),
                    onEmailChanged = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Check your inbox for the reset link.").assertIsDisplayed()
    }
}
