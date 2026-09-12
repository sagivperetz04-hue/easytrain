package com.easytrain.core.network.datasource

import com.easytrain.core.network.model.ProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

internal class SupabaseProfileNetworkDataSource
    @Inject
    constructor(
        private val client: SupabaseClient,
    ) : ProfileNetworkDataSource {
        override suspend fun getProfile(id: String): ProfileDto? =
            client
                .from(TABLE)
                .select {
                    filter { eq("id", id) }
                }.decodeSingleOrNull()

        override suspend fun upsertProfile(profile: ProfileDto) {
            client.from(TABLE).upsert(profile)
        }

        private companion object {
            const val TABLE = "profiles"
        }
    }
