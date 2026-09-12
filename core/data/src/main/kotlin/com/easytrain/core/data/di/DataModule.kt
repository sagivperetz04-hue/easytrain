package com.easytrain.core.data.di

import com.easytrain.core.data.repository.AuthRepository
import com.easytrain.core.data.repository.OfflineFirstProfileRepository
import com.easytrain.core.data.repository.ProfileRepository
import com.easytrain.core.data.repository.SupabaseAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindsAuthRepository(impl: SupabaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindsProfileRepository(impl: OfflineFirstProfileRepository): ProfileRepository
}
