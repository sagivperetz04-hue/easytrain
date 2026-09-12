package com.easytrain.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.easytrain.core.database.model.ProfileEntity
import com.easytrain.core.database.model.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id AND deleted_at IS NULL")
    fun observeById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE sync_state = 'PENDING'")
    suspend fun pending(): List<ProfileEntity>

    @Upsert
    suspend fun upsertAll(profiles: List<ProfileEntity>)

    @Query("SELECT id FROM profiles WHERE sync_state = 'PENDING'")
    suspend fun pendingIds(): List<String>

    /** Server rows never clobber a local write that has not been pushed yet. */
    @Transaction
    suspend fun upsertFromServer(profiles: List<ProfileEntity>) {
        val pending = pendingIds().toSet()
        upsertAll(profiles.filterNot { it.id in pending })
    }

    @Query("UPDATE profiles SET sync_state = :state WHERE id = :id")
    suspend fun markSyncState(
        id: String,
        state: SyncState,
    )

    @Query("DELETE FROM profiles")
    suspend fun clear()
}
