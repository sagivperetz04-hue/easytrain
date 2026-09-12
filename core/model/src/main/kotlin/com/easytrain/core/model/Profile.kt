package com.easytrain.core.model

import java.time.Instant

data class Profile(
    val id: String,
    val role: UserRole?,
    val displayName: String?,
    val avatarPath: String?,
    val units: Units,
    val timezone: String?,
    val updatedAt: Instant,
) {
    val hasRole: Boolean get() = role != null
    val isComplete: Boolean get() = role != null && !displayName.isNullOrBlank()
}
