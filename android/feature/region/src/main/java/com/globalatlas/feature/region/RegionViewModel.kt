package com.globalatlas.feature.region

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.globalatlas.core.network.AtlasApi
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class RegionListState(
    val countryIso2: String = "",
    val loading: Boolean = false,
    val regions: List<JsonObject> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class RegionViewModel @Inject constructor(private val api: AtlasApi) : ViewModel() {
    private val _state = MutableStateFlow(RegionListState())
    val state = _state.asStateFlow()

    fun load(countryIso2: String, retry: Boolean = false) {
        val code = countryIso2.trim().uppercase()
        if (code.length != 2) {
            _state.update { it.copy(countryIso2 = code, loading = false, error = "کد کشور معتبر نیست.") }
            return
        }
        if (!retry && _state.value.countryIso2 == code && (_state.value.loading || _state.value.regions.isNotEmpty())) return
        viewModelScope.launch {
            _state.update { it.copy(countryIso2 = code, loading = true, error = null) }
            try {
                val result = api.regions(code, 100, 0)
                _state.update { it.copy(loading = false, regions = result.filter { item -> item.isJsonObject }.map { item -> item.asJsonObject }) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = if (e is HttpException) "خطای سرور: ${e.code()}" else "دریافت فهرست استان‌ها ناموفق بود."
                _state.update { it.copy(loading = false, error = message) }
            }
        }
    }
}
