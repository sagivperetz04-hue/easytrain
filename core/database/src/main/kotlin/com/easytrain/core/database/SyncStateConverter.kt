package com.easytrain.core.database

import androidx.room.TypeConverter
import com.easytrain.core.database.model.SyncState

internal class SyncStateConverter {
    @TypeConverter
    fun fromSyncState(state: SyncState): String = state.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
}
