package com.easytrain.core.network.datasource

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

interface AuthNetworkDataSource {
    val sessionStatus: Flow<SessionStatus>

    fun currentUserId(): String?

    suspend fun signUp(
        email: String,
        password: String,
    )

    suspend fun signIn(
        email: String,
        password: String,
    )

    suspend fun signOut()

    suspend fun sendPasswordReset(email: String)
}
