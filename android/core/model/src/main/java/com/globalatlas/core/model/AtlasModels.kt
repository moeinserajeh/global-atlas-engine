package com.globalatlas.core.model

data class Country(
    val iso2: String,
    val iso3: String?,
    val nameEn: String,
    val nameFa: String? = null
)

data class AdministrativeRegion(
    val id: Long,
    val countryIso2: String,
    val nameEn: String,
    val nameFa: String? = null,
    val level: Int = 1
)
