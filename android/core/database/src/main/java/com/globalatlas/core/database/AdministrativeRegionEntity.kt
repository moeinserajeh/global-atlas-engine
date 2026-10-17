package com.globalatlas.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.globalatlas.core.model.AdministrativeRegion

@Entity(
    tableName = "administrative_regions",
    indices = [Index("countryIso2"), Index(value = ["countryIso2", "nameEn"])]
)
data class AdministrativeRegionEntity(
    @PrimaryKey val id: Long,
    val countryIso2: String,
    val nameEn: String,
    val nameFa: String?,
    val level: Int,
    val geometryJson: String?,
    val cachedAtEpochMillis: Long
) {
    fun toDomain() = AdministrativeRegion(id, countryIso2, nameEn, nameFa, level)
}
