package com.easytrain.feature.onboarding.signup

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
class SignUpViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SignUpUiState())
        val state: StateFlow<SignUpUiState> = _state.asStateFlow()

        fun onEmailChanged(email: String) = _state.update { it.copy(email = email, error = null) }

        fun onPasswordChanged(password: String) = _state.update { it.copy(password = password, error = null) }

        fun onSubmit() {
            if (!_state.value.canSubmit) return
            _state.update { it.copy(isSubmitting = true, error = null) }

            viewModelScope.launch {
                val current = _state.value
                when (val result = authRepository.signUp(current.email, current.password)) {
                    is AppResult.Success -> _state.update { it.copy(isSubmitting = false) }
                    is AppResult.Failure ->
                        _state.update { it.copy(isSubmitting = false, error = result.error) }
                }
            }
        }
    }
