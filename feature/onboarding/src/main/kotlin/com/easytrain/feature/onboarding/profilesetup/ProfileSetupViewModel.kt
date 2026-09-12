package com.easytrain.feature.onboarding.profilesetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easytrain.core.common.AppResult
import com.easytrain.core.data.repository.ProfileRepository
import com.easytrain.core.model.Units
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ProfileSetupViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ProfileSetupUiState())
        val state: StateFlow<ProfileSetupUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val profile = profileRepository.observeMe().first()
                if (profile != null) {
                    _state.update {
                        it.copy(displayName = profile.displayName.orEmpty(), units = profile.units)
                    }
                }
            }
        }

        fun onDisplayNameChanged(displayName: String) =
            _state.update { it.copy(displayName = displayName, error = null) }

        fun onUnitsChanged(units: Units) = _state.update { it.copy(units = units) }

        fun onSubmit(onDone: () -> Unit) {
            if (!_state.value.canSubmit) return
            _state.update { it.copy(isSubmitting = true, error = null) }

            viewModelScope.launch {
                val current = _state.value
                val result =
                    profileRepository.updateProfile(
                        displayName = current.displayName,
                        units = current.units,
                        timezone = ZoneId.systemDefault().id,
                    )
                when (result) {
                    is AppResult.Success -> {
                        _state.update { it.copy(isSubmitting = false) }
                        onDone()
                    }
                    is AppResult.Failure ->
                        _state.update { it.copy(isSubmitting = false, error = result.error) }
                }
            }
        }
    }
