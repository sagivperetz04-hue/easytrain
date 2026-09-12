package com.easytrain.core.database.model

/**
 * PENDING rows are local writes the server has not accepted yet; a pull must never overwrite them.
 */
enum class SyncState {
    SYNCED,
    PENDING,
    FAILED,
}
