package com.easytrain.feature.onboarding.chooserole

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import com.easytrain.core.designsystem.theme.Spacing
import com.easytrain.core.model.UserRole
import com.easytrain.core.ui.ErrorBanner
import com.easytrain.feature.onboarding.R

@Composable
fun ChooseRoleRoute(viewModel: ChooseRoleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChooseRoleScreen(state = state, onRoleChosen = viewModel::onRoleChosen)
}

@Composable
internal fun ChooseRoleScreen(
    state: ChooseRoleUiState,
    onRoleChosen: (UserRole) -> Unit,
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
                text = stringResource(R.string.onboarding_choose_role_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.onboarding_choose_role_subtitle),
                style = MaterialTheme.typography.bodyMedium,
            )

            state.error?.let { ErrorBanner(error = it) }

            RoleCard(
                title = stringResource(R.string.onboarding_choose_role_coach),
                detail = stringResource(R.string.onboarding_choose_role_coach_detail),
                enabled = !state.isSubmitting,
                onClick = { onRoleChosen(UserRole.COACH) },
            )

            RoleCard(
                title = stringResource(R.string.onboarding_choose_role_trainee),
                detail = stringResource(R.string.onboarding_choose_role_trainee_detail),
                enabled = !state.isSubmitting,
                onClick = { onRoleChosen(UserRole.TRAINEE) },
            )
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(name = "Choose role")
@Composable
private fun ChooseRoleScreenPreview() {
    EasyTrainTheme {
        ChooseRoleScreen(state = ChooseRoleUiState(), onRoleChosen = {})
    }
}
