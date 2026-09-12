package com.easytrain.feature.onboarding.chooserole

import com.easytrain.core.common.AppError

data class ChooseRoleUiState(
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
)
