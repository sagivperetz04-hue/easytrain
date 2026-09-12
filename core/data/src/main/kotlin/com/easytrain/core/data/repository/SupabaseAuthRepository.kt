package com.easytrain.core.data.repository

import com.easytrain.core.common.AppResult
import com.easytrain.core.common.Dispatcher
import com.easytrain.core.common.EasyTrainDispatcher
import com.easytrain.core.network.datasource.AuthNetworkDataSource
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SupabaseAuthRepository
    @Inject
    constructor(
        private val network: AuthNetworkDataSource,
        @Dispatcher(EasyTrainDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : AuthRepository {
        override val sessionState: Flow<SessionState> =
            network.sessionStatus.map { status ->
                when (status) {
                    is SessionStatus.Authenticated ->
                        SessionState.SignedIn(
                            status.session.user
                                ?.id
                                .orEmpty(),
                        )
                    is SessionStatus.Initializing -> SessionState.Loading
                    is SessionStatus.NotAuthenticated -> SessionState.SignedOut
                    is SessionStatus.RefreshFailure -> SessionState.SignedIn(network.currentUserId().orEmpty())
                }
            }

        override fun currentUserId(): String? = network.currentUserId()

        override suspend fun signUp(
            email: String,
            password: String,
        ): AppResult<Unit> = runCatchingAppResult { network.signUp(email.trim(), password) }

        override suspend fun signIn(
            email: String,
            password: String,
        ): AppResult<Unit> = runCatchingAppResult { network.signIn(email.trim(), password) }

        override suspend fun signOut(): AppResult<Unit> = runCatchingAppResult { network.signOut() }

        override suspend fun sendPasswordReset(email: String): AppResult<Unit> =
            runCatchingAppResult { network.sendPasswordReset(email.trim()) }

        private suspend fun runCatchingAppResult(block: suspend () -> Unit): AppResult<Unit> =
            withContext(ioDispatcher) {
                try {
                    block()
                    AppResult.Success(Unit)
                } catch (error: Exception) {
                    AppResult.Failure(error.asAppError())
                }
            }
    }
