package com.globalatlas.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.globalatlas.core.network.AtlasApi
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class SearchViewModel @Inject constructor(private val api: AtlasApi) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    fun dispatch(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                _state.update { it.copy(query = intent.value, error = null) }
                job?.cancel()
                val query = intent.value.trim()
                if (query.length < 2) _state.update { it.copy(results = emptyList(), isLoading = false) }
                else job = viewModelScope.launch { delay(300); search(query) }
            }
            SearchIntent.Clear -> {
                job?.cancel()
                _state.value = SearchState()
            }
            SearchIntent.Retry -> {
                val query = state.value.query.trim()
                if (query.length >= 2) { job?.cancel(); job = viewModelScope.launch { search(query) } }
            }
        }
    }

    private suspend fun search(query: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val countries = api.searchCountries(query, 15)
            val regions = api.searchRegions(query, 15)
            val result = buildList {
                countries.forEach { if (it.isJsonObject) add(it.asJsonObject.apply { addProperty("_type", "COUNTRY") }) }
                regions.forEach { if (it.isJsonObject) add(it.asJsonObject.apply { addProperty("_type", "REGION") }) }
            }.take(30)
            _state.update { it.copy(isLoading = false, results = result) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val message = if (e is HttpException) "خطای سرور: ${e.code()}" else "اتصال به سرور برقرار نشد."
            _state.update { it.copy(isLoading = false, error = message) }
        }
    }
}
