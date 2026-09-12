package com.easytrain.feature.onboarding.chooserole

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easytrain.core.common.AppResult
import com.easytrain.core.data.repository.ProfileRepository
import com.easytrain.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * On success the app moves on by itself: the profile flow reports a role and the start destination
 * changes. Nothing here navigates.
 */
@HiltViewModel
class ChooseRoleViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ChooseRoleUiState())
        val state: StateFlow<ChooseRoleUiState> = _state.asStateFlow()

        fun onRoleChosen(role: UserRole) {
            if (_state.value.isSubmitting) return
            _state.update { it.copy(isSubmitting = true, error = null) }

            viewModelScope.launch {
                when (val result = profileRepository.setRole(role)) {
                    is AppResult.Success -> _state.update { it.copy(isSubmitting = false) }
                    is AppResult.Failure ->
                        _state.update { it.copy(isSubmitting = false, error = result.error) }
                }
            }
        }
    }
