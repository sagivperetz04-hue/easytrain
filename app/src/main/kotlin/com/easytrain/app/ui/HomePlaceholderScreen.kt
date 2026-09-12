package com.easytrain.app.ui

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
import com.easytrain.app.R
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import com.easytrain.core.designsystem.theme.Spacing
import com.easytrain.core.model.UserRole

/** Replaced by the coach hub and the trainee Today screen in ET-004 and ET-006. */
@Composable
fun HomePlaceholderScreen(
    role: UserRole,
    onSignOut: () -> Unit,
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
                text =
                    when (role) {
                        UserRole.COACH -> stringResource(R.string.app_home_coach)
                        UserRole.TRAINEE -> stringResource(R.string.app_home_trainee)
                    },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_home_placeholder),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.app_sign_out))
            }
        }
    }
}

@Preview(name = "Coach home")
@Composable
private fun HomePlaceholderScreenPreview() {
    EasyTrainTheme {
        HomePlaceholderScreen(role = UserRole.COACH, onSignOut = {})
    }
}
