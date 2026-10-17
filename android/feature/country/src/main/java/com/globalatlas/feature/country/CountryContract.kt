package com.globalatlas.feature.country

import com.google.gson.JsonObject

data class CountryDetailState(
    val isLoading: Boolean = false,
    val country: JsonObject? = null,
    val error: String? = null
)

sealed interface CountryDetailIntent {
    data class Load(val iso2: String) : CountryDetailIntent
    data object Retry : CountryDetailIntent
}
