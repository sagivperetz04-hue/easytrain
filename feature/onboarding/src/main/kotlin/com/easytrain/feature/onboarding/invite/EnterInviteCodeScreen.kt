package com.easytrain.feature.onboarding.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import com.easytrain.core.designsystem.theme.Spacing
import com.easytrain.feature.onboarding.R

/** Stub until ET-003 brings invites and the coach/trainee link. */
@Composable
fun EnterInviteCodeRoute(onSkip: () -> Unit) {
    EnterInviteCodeScreen(onSkip = onSkip)
}

@Composable
internal fun EnterInviteCodeScreen(
    onSkip: () -> Unit,
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
                text = stringResource(R.string.onboarding_invite_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.onboarding_invite_stub),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_invite_skip))
            }
        }
    }
}

@Preview(name = "Enter invite code")
@Composable
private fun EnterInviteCodeScreenPreview() {
    EasyTrainTheme {
        EnterInviteCodeScreen(onSkip = {})
    }
}
