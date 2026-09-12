package com.easytrain.feature.onboarding.forgotpassword

import com.easytrain.core.common.AppError

data class ForgotPasswordUiState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val isSent: Boolean = false,
    val error: AppError? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && !isSubmitting
}
