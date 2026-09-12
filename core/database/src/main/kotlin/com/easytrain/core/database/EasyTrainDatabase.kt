package com.easytrain.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.easytrain.core.database.dao.ProfileDao
import com.easytrain.core.database.model.ProfileEntity

@Database(
    entities = [ProfileEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(SyncStateConverter::class)
abstract class EasyTrainDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
