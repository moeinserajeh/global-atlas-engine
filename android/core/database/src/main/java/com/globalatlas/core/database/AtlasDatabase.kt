package com.globalatlas.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CountryEntity::class, AdministrativeRegionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AtlasDatabase : RoomDatabase() {
    abstract fun atlasDao(): AtlasDao
}
