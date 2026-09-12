package com.easytrain.feature.onboarding.signup

import com.easytrain.core.common.AppError

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
) {
    val passwordTooShort: Boolean
        get() = password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH

    val canSubmit: Boolean
        get() = email.isNotBlank() && password.length >= MIN_PASSWORD_LENGTH && !isSubmitting

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
