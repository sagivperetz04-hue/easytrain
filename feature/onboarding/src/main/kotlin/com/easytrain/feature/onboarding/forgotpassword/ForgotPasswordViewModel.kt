package com.easytrain.feature.onboarding.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easytrain.core.common.AppResult
import com.easytrain.core.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ForgotPasswordUiState())
        val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

        fun onEmailChanged(email: String) = _state.update { it.copy(email = email, error = null, isSent = false) }

        fun onSubmit() {
            if (!_state.value.canSubmit) return
            _state.update { it.copy(isSubmitting = true, error = null) }

            viewModelScope.launch {
                when (val result = authRepository.sendPasswordReset(_state.value.email)) {
                    is AppResult.Success -> _state.update { it.copy(isSubmitting = false, isSent = true) }
                    is AppResult.Failure ->
                        _state.update { it.copy(isSubmitting = false, error = result.error) }
                }
            }
        }
    }
