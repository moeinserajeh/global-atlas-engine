package com.globalatlas.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CountryEntity::class, AdministrativeRegionEntity::class, FavoriteEntity::class, RecentViewEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AtlasDatabase : RoomDatabase() {
    abstract fun atlasDao(): AtlasDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS favorites (
                    entityType TEXT NOT NULL, entityId TEXT NOT NULL, countryIso2 TEXT,
                    displayName TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(entityType, entityId))""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_favorites_createdAtEpochMillis ON favorites(createdAtEpochMillis)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS recent_views (
                    entityType TEXT NOT NULL, entityId TEXT NOT NULL, countryIso2 TEXT,
                    displayName TEXT NOT NULL, viewedAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(entityType, entityId))""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_views_viewedAtEpochMillis ON recent_views(viewedAtEpochMillis)")
            }
        }
    }
}
