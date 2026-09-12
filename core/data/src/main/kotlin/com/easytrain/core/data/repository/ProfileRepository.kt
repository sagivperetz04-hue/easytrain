package com.easytrain.core.data.repository

import com.easytrain.core.common.AppResult
import com.easytrain.core.model.Profile
import com.easytrain.core.model.Units
import com.easytrain.core.model.UserRole
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** Reads come from Room, so the UI renders instantly and works offline. */
    fun observeMe(): Flow<Profile?>

    suspend fun refreshMe(): AppResult<Unit>

    /** The role can only be set while it is still null; the server rejects a second attempt. */
    suspend fun setRole(role: UserRole): AppResult<Unit>

    suspend fun updateProfile(
        displayName: String,
        units: Units,
        timezone: String?,
    ): AppResult<Unit>

    suspend fun clearLocalData()
}
