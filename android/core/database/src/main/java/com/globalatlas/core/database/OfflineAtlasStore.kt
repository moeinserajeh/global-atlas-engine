package com.globalatlas.core.database

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Persistence facade used by domain repositories. Geometry is stored as GeoJSON text
 * so a downloaded region can be rendered without a network request.
 */
@Singleton
class OfflineAtlasStore @Inject constructor(private val dao: AtlasDao) {
    fun observeCountries(): Flow<List<CountryEntity>> = dao.observeCountries()
    fun searchCountries(query: String): Flow<List<CountryEntity>> =
        dao.searchCountries(query.trim(), limit = 50)
    fun observeRegions(countryIso2: String): Flow<List<AdministrativeRegionEntity>> =
        dao.observeRegions(countryIso2.uppercase())

    suspend fun cacheCountries(items: List<CountryEntity>) = dao.upsertCountries(items)

    suspend fun replaceCountryRegions(countryIso2: String, items: List<AdministrativeRegionEntity>) {
        val normalized = countryIso2.uppercase()
        require(items.all { it.countryIso2.equals(normalized, ignoreCase = true) }) {
            "Every cached region must belong to the requested country"
        }
        dao.deleteRegionsForCountry(normalized)
        dao.upsertRegions(items)
    }

    suspend fun setFavorite(type: String, id: String, countryIso2: String?, name: String, now: Long) {
        require(type == "COUNTRY" || type == "REGION")
        require(id.isNotBlank() && name.isNotBlank())
        dao.putFavorite(FavoriteEntity(type, id, countryIso2?.uppercase(), name, now))
    }

    suspend fun removeFavorite(type: String, id: String) = dao.removeFavorite(type, id)
    fun observeFavorites(): Flow<List<FavoriteEntity>> = dao.observeFavorites()

    suspend fun recordViewed(type: String, id: String, countryIso2: String?, name: String, now: Long) {
        require(type == "COUNTRY" || type == "REGION")
        require(id.isNotBlank() && name.isNotBlank())
        dao.recordRecent(RecentViewEntity(type, id, countryIso2?.uppercase(), name, now))
    }

    fun observeRecent(limit: Int = 50): Flow<List<RecentViewEntity>> =
        dao.observeRecent(limit.coerceIn(1, 100))

    suspend fun clearRecent() = dao.clearRecent()
}
