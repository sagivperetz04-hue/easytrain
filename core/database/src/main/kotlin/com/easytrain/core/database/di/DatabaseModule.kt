package com.easytrain.core.database.di

import android.content.Context
import androidx.room.Room
import com.easytrain.core.database.EasyTrainDatabase
import com.easytrain.core.database.dao.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(
        @ApplicationContext context: Context,
    ): EasyTrainDatabase = Room.databaseBuilder(context, EasyTrainDatabase::class.java, "easytrain.db").build()

    @Provides
    fun providesProfileDao(database: EasyTrainDatabase): ProfileDao = database.profileDao()
}
