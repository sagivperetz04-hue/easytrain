package com.easytrain.app.di

import com.easytrain.app.BuildConfig
import com.easytrain.core.network.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * A library module cannot read the application's BuildConfig, so the values enter the graph here.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseConfigModule {
    @Provides
    @Singleton
    fun providesSupabaseConfig(): SupabaseConfig =
        SupabaseConfig(
            url = BuildConfig.SUPABASE_URL,
            publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        )
}
