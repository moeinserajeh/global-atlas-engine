package com.globalatlas.feature.search

import com.google.gson.JsonObject

data class SearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<JsonObject> = emptyList(),
    val error: String? = null
)

sealed interface SearchIntent {
    data class QueryChanged(val value: String) : SearchIntent
    data object Clear : SearchIntent
    data object Retry : SearchIntent
}
