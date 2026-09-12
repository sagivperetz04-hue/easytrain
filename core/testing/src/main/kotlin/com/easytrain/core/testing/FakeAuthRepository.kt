package com.easytrain.core.testing

import com.easytrain.core.common.AppError
import com.easytrain.core.common.AppResult
import com.easytrain.core.data.repository.AuthRepository
import com.easytrain.core.data.repository.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val sessionState: Flow<SessionState> = _sessionState

    var nextResult: AppResult<Unit> = AppResult.Success(Unit)
    var signedInWith: Pair<String, String>? = null
        private set
    var signOutCount: Int = 0
        private set
    var passwordResetEmail: String? = null
        private set

    fun setSession(state: SessionState) {
        _sessionState.value = state
    }

    fun failWith(error: AppError) {
        nextResult = AppResult.Failure(error)
    }

    override fun currentUserId(): String? = (_sessionState.value as? SessionState.SignedIn)?.userId

    override suspend fun signUp(
        email: String,
        password: String,
    ): AppResult<Unit> {
        signedInWith = email to password
        return nextResult
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): AppResult<Unit> {
        signedInWith = email to password
        return nextResult
    }

    override suspend fun signOut(): AppResult<Unit> {
        signOutCount++
        return nextResult
    }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> {
        passwordResetEmail = email
        return nextResult
    }
}
