package com.easytrain.feature.onboarding.signin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.easytrain.core.common.AppError
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SignInScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the submit button is disabled until both fields are filled`() {
        composeTestRule.setContent {
            EasyTrainTheme {
                SignInScreen(
                    state = SignInUiState(),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onSignUp = {},
                    onForgotPassword = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(SIGN_IN_SUBMIT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SIGN_IN_SUBMIT_TAG).assertIsNotEnabled()
    }

    @Test
    fun `a filled form enables submit and reports the tap`() {
        var submitted = false
        composeTestRule.setContent {
            EasyTrainTheme {
                SignInScreen(
                    state = SignInUiState(email = "dana@example.com", password = "secret123"),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = { submitted = true },
                    onSignUp = {},
                    onForgotPassword = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(SIGN_IN_SUBMIT_TAG).assertIsEnabled().performClick()

        assertTrue(submitted)
    }

    @Test
    fun `a rejected sign-in shows the error`() {
        composeTestRule.setContent {
            EasyTrainTheme {
                SignInScreen(
                    state = SignInUiState(error = AppError.InvalidCredentials),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onSubmit = {},
                    onSignUp = {},
                    onForgotPassword = {},
                )
            }
        }

        composeTestRule.onNodeWithText("That email and password don't match.").assertIsDisplayed()
    }
}
