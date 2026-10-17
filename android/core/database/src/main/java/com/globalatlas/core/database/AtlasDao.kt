package com.globalatlas.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AtlasDao {
    @Query("SELECT * FROM countries ORDER BY nameEn COLLATE NOCASE")
    fun observeCountries(): Flow<List<CountryEntity>>
    @Query("SELECT * FROM countries WHERE iso2 = :iso2 LIMIT 1")
    fun observeCountry(iso2: String): Flow<CountryEntity?>
    @Query("SELECT * FROM countries WHERE nameEn LIKE '%' || :query || '%' OR nameFa LIKE '%' || :query || '%' OR iso2 LIKE '%' || :query || '%' ORDER BY nameEn LIMIT :limit")
    fun searchCountries(query: String, limit: Int = 50): Flow<List<CountryEntity>>
    @Query("SELECT * FROM administrative_regions WHERE countryIso2 = :iso2 ORDER BY nameEn COLLATE NOCASE")
    fun observeRegions(iso2: String): Flow<List<AdministrativeRegionEntity>>
    @Query("SELECT * FROM administrative_regions WHERE id = :id LIMIT 1")
    suspend fun region(id: Long): AdministrativeRegionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCountries(items: List<CountryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegions(items: List<AdministrativeRegionEntity>)
    @Query("DELETE FROM administrative_regions WHERE countryIso2 = :iso2")
    suspend fun deleteRegionsForCountry(iso2: String)
    @Query("DELETE FROM countries WHERE iso2 = :iso2")
    suspend fun deleteCountry(iso2: String)
    @Query("SELECT COUNT(*) FROM countries")
    suspend fun countryCount(): Int
    @Query("SELECT COUNT(*) FROM administrative_regions")
    suspend fun regionCount(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFavorite(item: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE entityType = :type AND entityId = :id")
    suspend fun removeFavorite(type: String, id: String)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE entityType = :type AND entityId = :id)")
    fun observeIsFavorite(type: String, id: String): Flow<Boolean>
    @Query("SELECT * FROM favorites ORDER BY createdAtEpochMillis DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>
    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordRecent(item: RecentViewEntity)
    @Query("SELECT * FROM recent_views ORDER BY viewedAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<RecentViewEntity>>
    @Query("DELETE FROM recent_views WHERE entityType = :type AND entityId = :id")
    suspend fun removeRecent(type: String, id: String)
    @Query("DELETE FROM recent_views")
    suspend fun clearRecent()
}
