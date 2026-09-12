package com.easytrain.feature.onboarding.profilesetup

import com.easytrain.core.common.AppError
import com.easytrain.core.model.Units

data class ProfileSetupUiState(
    val displayName: String = "",
    val units: Units = Units.KG,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
) {
    val canSubmit: Boolean
        get() = displayName.isNotBlank() && !isSubmitting
}
