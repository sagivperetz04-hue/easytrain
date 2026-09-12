package com.easytrain.core.network.datasource

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class SupabaseAuthNetworkDataSource
    @Inject
    constructor(
        private val client: SupabaseClient,
    ) : AuthNetworkDataSource {
        override val sessionStatus: Flow<SessionStatus> = client.auth.sessionStatus

        override fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

        override suspend fun signUp(
            email: String,
            password: String,
        ) {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }

        override suspend fun signIn(
            email: String,
            password: String,
        ) {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

        override suspend fun signOut() {
            client.auth.signOut()
        }

        override suspend fun sendPasswordReset(email: String) {
            client.auth.resetPasswordForEmail(email)
        }
    }
