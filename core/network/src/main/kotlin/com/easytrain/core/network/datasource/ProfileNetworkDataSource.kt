package com.easytrain.core.network.datasource

import com.easytrain.core.network.model.ProfileDto

interface ProfileNetworkDataSource {
    suspend fun getProfile(id: String): ProfileDto?

    suspend fun upsertProfile(profile: ProfileDto)
}
