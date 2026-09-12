package com.easytrain.feature.onboarding.profilesetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import com.easytrain.core.designsystem.theme.Spacing
import com.easytrain.core.model.Units
import com.easytrain.core.ui.ErrorBanner
import com.easytrain.feature.onboarding.R

@Composable
fun ProfileSetupRoute(
    onDone: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileSetupScreen(
        state = state,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        onUnitsChanged = viewModel::onUnitsChanged,
        onSubmit = { viewModel.onSubmit(onDone) },
    )
}

@Composable
internal fun ProfileSetupScreen(
    state: ProfileSetupUiState,
    onDisplayNameChanged: (String) -> Unit,
    onUnitsChanged: (Units) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.onboarding_profile_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            state.error?.let { ErrorBanner(error = it) }

            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChanged,
                label = { Text(stringResource(R.string.onboarding_profile_name)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.onboarding_profile_units),
                style = MaterialTheme.typography.titleMedium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(
                    selected = state.units == Units.KG,
                    onClick = { onUnitsChanged(Units.KG) },
                    label = { Text(stringResource(R.string.onboarding_profile_units_kg)) },
                )
                FilterChip(
                    selected = state.units == Units.LB,
                    onClick = { onUnitsChanged(Units.LB) },
                    label = { Text(stringResource(R.string.onboarding_profile_units_lb)) },
                )
            }

            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.onboarding_profile_save))
                }
            }
        }
    }
}

@Preview(name = "Profile setup")
@Composable
private fun ProfileSetupScreenPreview() {
    EasyTrainTheme {
        ProfileSetupScreen(
            state = ProfileSetupUiState(displayName = "Dana"),
            onDisplayNameChanged = {},
            onUnitsChanged = {},
            onSubmit = {},
        )
    }
}
