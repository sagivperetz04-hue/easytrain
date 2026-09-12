package com.easytrain.core.testing

import com.easytrain.core.common.AppError
import com.easytrain.core.common.AppResult
import com.easytrain.core.data.repository.ProfileRepository
import com.easytrain.core.model.Profile
import com.easytrain.core.model.Units
import com.easytrain.core.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class FakeProfileRepository : ProfileRepository {
    private val profile = MutableStateFlow<Profile?>(null)

    var nextResult: AppResult<Unit> = AppResult.Success(Unit)
    var roleSet: UserRole? = null
        private set
    var savedProfile: Triple<String, Units, String?>? = null
        private set
    var clearCount: Int = 0
        private set

    fun emit(value: Profile?) {
        profile.value = value
    }

    fun failWith(error: AppError) {
        nextResult = AppResult.Failure(error)
    }

    override fun observeMe(): Flow<Profile?> = profile

    override suspend fun refreshMe(): AppResult<Unit> = nextResult

    override suspend fun setRole(role: UserRole): AppResult<Unit> {
        if (nextResult is AppResult.Success) roleSet = role
        return nextResult
    }

    override suspend fun updateProfile(
        displayName: String,
        units: Units,
        timezone: String?,
    ): AppResult<Unit> {
        if (nextResult is AppResult.Success) savedProfile = Triple(displayName, units, timezone)
        return nextResult
    }

    override suspend fun clearLocalData() {
        clearCount++
    }
}

fun testProfile(
    id: String = "user-1",
    role: UserRole? = null,
    displayName: String? = null,
    units: Units = Units.KG,
): Profile =
    Profile(
        id = id,
        role = role,
        displayName = displayName,
        avatarPath = null,
        units = units,
        timezone = "Asia/Jerusalem",
        updatedAt = Instant.EPOCH,
    )
