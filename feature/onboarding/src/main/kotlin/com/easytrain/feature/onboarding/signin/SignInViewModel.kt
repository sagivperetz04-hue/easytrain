package com.easytrain.feature.onboarding.signin

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

/**
 * Signing in has no local fallback, so this screen is the one place that shows a spinner: the app
 * cannot show anything meaningful until the session exists.
 */
@HiltViewModel
class SignInViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SignInUiState())
        val state: StateFlow<SignInUiState> = _state.asStateFlow()

        fun onEmailChanged(email: String) = _state.update { it.copy(email = email, error = null) }

        fun onPasswordChanged(password: String) = _state.update { it.copy(password = password, error = null) }

        fun onSubmit() {
            if (!_state.value.canSubmit) return
            _state.update { it.copy(isSubmitting = true, error = null) }

            viewModelScope.launch {
                val current = _state.value
                when (val result = authRepository.signIn(current.email, current.password)) {
                    is AppResult.Success -> _state.update { it.copy(isSubmitting = false) }
                    is AppResult.Failure ->
                        _state.update { it.copy(isSubmitting = false, error = result.error) }
                }
            }
        }
    }
