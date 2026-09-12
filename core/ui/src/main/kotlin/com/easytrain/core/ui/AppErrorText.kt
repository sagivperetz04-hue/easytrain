package com.easytrain.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.easytrain.core.common.AppError

/** One place that turns a failure into words a person can act on. */
@Composable
fun AppError.asText(): String =
    when (this) {
        AppError.Offline -> stringResource(R.string.ui_error_offline)
        AppError.InvalidCredentials -> stringResource(R.string.ui_error_invalid_credentials)
        AppError.EmailAlreadyRegistered -> stringResource(R.string.ui_error_email_taken)
        AppError.EmailNotConfirmed -> stringResource(R.string.ui_error_email_not_confirmed)
        AppError.NotAuthenticated -> stringResource(R.string.ui_error_not_authenticated)
        is AppError.Validation -> message
        is AppError.Unexpected -> stringResource(R.string.ui_error_unexpected)
    }
