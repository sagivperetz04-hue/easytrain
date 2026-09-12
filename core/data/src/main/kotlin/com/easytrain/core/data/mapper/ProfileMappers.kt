package com.easytrain.core.data.mapper

import com.easytrain.core.database.model.ProfileEntity
import com.easytrain.core.database.model.SyncState
import com.easytrain.core.model.Profile
import com.easytrain.core.model.Units
import com.easytrain.core.model.UserRole
import com.easytrain.core.network.model.ProfileDto
import java.time.Instant

fun ProfileDto.asEntity(syncState: SyncState = SyncState.SYNCED): ProfileEntity =
    ProfileEntity(
        id = id,
        role = role,
        displayName = displayName,
        avatarPath = avatarPath,
        units = units,
        timezone = timezone,
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli(),
        deletedAt = deletedAt?.let { Instant.parse(it).toEpochMilli() },
        syncState = syncState,
        localUpdatedAt = System.currentTimeMillis(),
    )

fun ProfileEntity.asDto(): ProfileDto =
    ProfileDto(
        id = id,
        role = role,
        displayName = displayName,
        avatarPath = avatarPath,
        units = units,
        timezone = timezone,
        createdAt = Instant.ofEpochMilli(createdAt).toString(),
        updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
        deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
    )

fun ProfileEntity.asExternalModel(): Profile =
    Profile(
        id = id,
        role = UserRole.fromWireValue(role),
        displayName = displayName,
        avatarPath = avatarPath,
        units = Units.fromWireValue(units),
        timezone = timezone,
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
