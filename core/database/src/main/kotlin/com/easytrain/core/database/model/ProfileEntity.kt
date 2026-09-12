package com.easytrain.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey override val id: String,
    @ColumnInfo(name = "role") val role: String?,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "avatar_path") val avatarPath: String?,
    @ColumnInfo(name = "units") val units: String,
    @ColumnInfo(name = "timezone") val timezone: String?,
    @ColumnInfo(name = "created_at") override val createdAt: Long,
    @ColumnInfo(name = "updated_at") override val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") override val deletedAt: Long?,
    @ColumnInfo(name = "sync_state") override val syncState: SyncState,
    @ColumnInfo(name = "local_updated_at") override val localUpdatedAt: Long,
) : SyncedEntity
