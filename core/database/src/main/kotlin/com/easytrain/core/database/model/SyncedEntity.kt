package com.easytrain.core.database.model

/** Columns every synced table carries. Timestamps are epoch millis in Room, ISO-8601 on the wire. */
interface SyncedEntity {
    val id: String
    val createdAt: Long
    val updatedAt: Long
    val deletedAt: Long?
    val syncState: SyncState
    val localUpdatedAt: Long
}
