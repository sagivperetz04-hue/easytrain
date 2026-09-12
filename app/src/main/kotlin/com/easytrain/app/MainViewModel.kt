package com.easytrain.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easytrain.core.data.repository.AuthRepository
import com.easytrain.core.data.repository.ProfileRepository
import com.easytrain.core.data.repository.SessionState
import com.easytrain.core.model.Profile
import com.easytrain.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the app should show before any screen decides anything. */
sealed interface AppUiState {
    data object Loading : AppUiState

    data object SignedOut : AppUiState

    data object NeedsRole : AppUiState

    data object NeedsProfile : AppUiState

    data class Ready(
        val role: UserRole,
    ) : AppUiState
}

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        val state: StateFlow<AppUiState> =
            combine(
                authRepository.sessionState.onEach { session ->
                    // The profile row is created server-side at signup; pull it as soon as we have a session.
                    if (session is SessionState.SignedIn) profileRepository.refreshMe()
                },
                profileRepository.observeMe(),
            ) { session, profile -> session to profile }
                .map { (session, profile) -> session.toAppUiState(profile) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = AppUiState.Loading,
                )

        fun onSignOut() {
            viewModelScope.launch {
                authRepository.signOut()
                profileRepository.clearLocalData()
            }
        }

        private fun SessionState.toAppUiState(profile: Profile?): AppUiState =
            when (this) {
                SessionState.Loading -> AppUiState.Loading
                SessionState.SignedOut -> AppUiState.SignedOut
                is SessionState.SignedIn -> {
                    val role = profile?.role
                    when {
                        profile == null -> AppUiState.Loading
                        role == null -> AppUiState.NeedsRole
                        profile.displayName.isNullOrBlank() -> AppUiState.NeedsProfile
                        else -> AppUiState.Ready(role)
                    }
                }
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
