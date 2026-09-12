package com.easytrain.core.data.repository

import com.easytrain.core.common.AppError
import com.easytrain.core.common.AppResult
import com.easytrain.core.common.Dispatcher
import com.easytrain.core.common.EasyTrainDispatcher
import com.easytrain.core.data.mapper.asEntity
import com.easytrain.core.data.mapper.asExternalModel
import com.easytrain.core.database.dao.ProfileDao
import com.easytrain.core.database.model.SyncState
import com.easytrain.core.model.Profile
import com.easytrain.core.model.Units
import com.easytrain.core.model.UserRole
import com.easytrain.core.network.datasource.ProfileNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads come from Room. Writes go to the server first and the response is what lands locally —
 * ET-003 moves them onto the sync engine so they work offline too.
 */
@Singleton
internal class OfflineFirstProfileRepository
    @Inject
    constructor(
        private val dao: ProfileDao,
        private val network: ProfileNetworkDataSource,
        private val authRepository: AuthRepository,
        @Dispatcher(EasyTrainDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : ProfileRepository {
        override fun observeMe(): Flow<Profile?> =
            authRepository.sessionState.flatMapLatest { session ->
                when (session) {
                    is SessionState.SignedIn -> dao.observeById(session.userId).map { it?.asExternalModel() }
                    else -> flowOf(null)
                }
            }

        override suspend fun refreshMe(): AppResult<Unit> =
            withUserId { userId ->
                val remote = network.getProfile(userId) ?: return@withUserId
                dao.upsertFromServer(listOf(remote.asEntity(SyncState.SYNCED)))
            }

        override suspend fun setRole(role: UserRole): AppResult<Unit> =
            withUserId { userId ->
                val current = requireRemoteProfile(userId)
                network.upsertProfile(current.copy(role = role.wireValue))
                pullInto(userId)
            }

        override suspend fun updateProfile(
            displayName: String,
            units: Units,
            timezone: String?,
        ): AppResult<Unit> =
            withUserId { userId ->
                val current = requireRemoteProfile(userId)
                network.upsertProfile(
                    current.copy(
                        displayName = displayName.trim(),
                        units = units.wireValue,
                        timezone = timezone,
                    ),
                )
                pullInto(userId)
            }

        override suspend fun clearLocalData() {
            withContext(ioDispatcher) { dao.clear() }
        }

        private suspend fun requireRemoteProfile(userId: String) =
            network.getProfile(userId) ?: error("The signed-in user has no profile row")

        private suspend fun pullInto(userId: String) {
            val refreshed = network.getProfile(userId) ?: return
            dao.upsertAll(listOf(refreshed.asEntity(SyncState.SYNCED)))
        }

        private suspend fun withUserId(block: suspend (String) -> Unit): AppResult<Unit> =
            withContext(ioDispatcher) {
                val userId =
                    authRepository.currentUserId()
                        ?: return@withContext AppResult.Failure(AppError.NotAuthenticated)
                try {
                    block(userId)
                    AppResult.Success(Unit)
                } catch (error: Exception) {
                    AppResult.Failure(error.asAppError())
                }
            }
    }
