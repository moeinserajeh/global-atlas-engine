package com.globalatlas.feature.country

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.globalatlas.core.network.AtlasApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class CountryViewModel @Inject constructor(private val api: AtlasApi) : ViewModel() {
    private val _state = MutableStateFlow(CountryDetailState())
    val state = _state.asStateFlow()
    private var currentIso2: String? = null

    fun dispatch(intent: CountryDetailIntent) {
        when (intent) {
            is CountryDetailIntent.Load -> {
                currentIso2 = intent.iso2.trim().uppercase()
                load()
            }
            CountryDetailIntent.Retry -> load()
        }
    }

    private fun load() {
        val iso2 = currentIso2 ?: return
        if (iso2.length != 2) {
            _state.update { it.copy(isLoading = false, error = "کد کشور معتبر نیست.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val country = api.country(iso2)
                _state.update { it.copy(isLoading = false, country = country) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = if (e is HttpException) "خطای سرور: ${e.code()}" else "دریافت اطلاعات کشور ناموفق بود."
                _state.update { it.copy(isLoading = false, error = message) }
            }
        }
    }
}
