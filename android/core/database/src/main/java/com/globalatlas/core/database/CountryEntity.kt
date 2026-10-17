package com.globalatlas.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.globalatlas.core.model.Country

@Entity(tableName = "countries")
data class CountryEntity(
    @PrimaryKey val iso2: String,
    val iso3: String?,
    val nameEn: String,
    val nameFa: String?,
    val cachedAtEpochMillis: Long
) {
    fun toDomain() = Country(iso2 = iso2, iso3 = iso3, nameEn = nameEn, nameFa = nameFa)

    companion object {
        fun fromDomain(country: Country, now: Long) = CountryEntity(
            iso2 = country.iso2.uppercase(),
            iso3 = country.iso3,
            nameEn = country.nameEn,
            nameFa = country.nameFa,
            cachedAtEpochMillis = now
        )
    }
}
