package com.easytrain.core.network.di

import com.easytrain.core.network.datasource.AuthNetworkDataSource
import com.easytrain.core.network.datasource.ProfileNetworkDataSource
import com.easytrain.core.network.datasource.SupabaseAuthNetworkDataSource
import com.easytrain.core.network.datasource.SupabaseProfileNetworkDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataSourceModule {
    @Binds
    @Singleton
    abstract fun bindsAuthNetworkDataSource(impl: SupabaseAuthNetworkDataSource): AuthNetworkDataSource

    @Binds
    @Singleton
    abstract fun bindsProfileNetworkDataSource(impl: SupabaseProfileNetworkDataSource): ProfileNetworkDataSource
}
