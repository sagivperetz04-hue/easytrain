package com.easytrain.core.data.repository

import com.easytrain.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/** What the app knows about the current session, independent of Supabase types. */
sealed interface SessionState {
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(
        val userId: String,
    ) : SessionState
}

interface AuthRepository {
    val sessionState: Flow<SessionState>

    fun currentUserId(): String?

    suspend fun signUp(
        email: String,
        password: String,
    ): AppResult<Unit>

    suspend fun signIn(
        email: String,
        password: String,
    ): AppResult<Unit>

    suspend fun signOut(): AppResult<Unit>

    suspend fun sendPasswordReset(email: String): AppResult<Unit>
}
