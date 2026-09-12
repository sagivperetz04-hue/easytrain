package com.easytrain.feature.onboarding.signup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
class SignUpScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a short password is called out and blocks submission`() {
        composeTestRule.setContent {
            EasyTrainTheme {
                SignUpScreen(
                    state = SignUpUiState(email = "dana@example.com", password = "short"),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onSignIn = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Use at least 8 characters.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign up").assertIsNotEnabled()
    }
}
