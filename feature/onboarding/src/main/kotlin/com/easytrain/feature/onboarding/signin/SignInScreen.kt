package com.easytrain.feature.onboarding.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easytrain.core.common.AppError
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import com.easytrain.core.designsystem.theme.Spacing
import com.easytrain.core.ui.ErrorBanner
import com.easytrain.feature.onboarding.R

/** The headline and the button share their words, so the action carries a tag for tests. */
internal const val SIGN_IN_SUBMIT_TAG = "signIn:submit"

@Composable
fun SignInRoute(
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SignInScreen(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSubmit = viewModel::onSubmit,
        onSignUp = onSignUp,
        onForgotPassword = onForgotPassword,
    )
}

@Composable
internal fun SignInScreen(
    state: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.onboarding_sign_in_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            state.error?.let { ErrorBanner(error = it) }

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChanged,
                label = { Text(stringResource(R.string.onboarding_email)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChanged,
                label = { Text(stringResource(R.string.onboarding_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(SIGN_IN_SUBMIT_TAG),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.onboarding_sign_in_action))
                }
            }

            TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_forgot_password_link))
            }

            TextButton(onClick = onSignUp, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_sign_in_to_sign_up))
            }
        }
    }
}

@Preview(name = "Sign in")
@Composable
private fun SignInScreenPreview() {
    EasyTrainTheme {
        SignInScreen(
            state = SignInUiState(email = "dana@example.com", password = "secret123"),
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmit = {},
            onSignUp = {},
            onForgotPassword = {},
        )
    }
}

@Preview(name = "Sign in — error")
@Composable
private fun SignInScreenErrorPreview() {
    EasyTrainTheme {
        SignInScreen(
            state = SignInUiState(email = "dana@example.com", error = AppError.InvalidCredentials),
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmit = {},
            onSignUp = {},
            onForgotPassword = {},
        )
    }
}
