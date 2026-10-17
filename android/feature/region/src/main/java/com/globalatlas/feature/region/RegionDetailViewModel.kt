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
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class RegionDetailState(val loading: Boolean = false, val detail: JsonObject? = null, val error: String? = null)

@HiltViewModel
class RegionDetailViewModel @Inject constructor(private val api: AtlasApi) : ViewModel() {
    private val _state = MutableStateFlow(RegionDetailState())
    val state = _state.asStateFlow()
    private var currentId: Long? = null

    fun load(id: Long, retry: Boolean = false) {
        if (id < 1) { _state.value = RegionDetailState(error = "شناسهٔ منطقه معتبر نیست."); return }
        if (!retry && currentId == id && (_state.value.loading || _state.value.detail != null)) return
        currentId = id
        viewModelScope.launch {
            _state.value = RegionDetailState(loading = true)
            try { _state.value = RegionDetailState(detail = api.region(id)) }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = if (e is HttpException) "خطای سرور: \${e.code()}" else "دریافت جزئیات منطقه ناموفق بود."
                _state.value = RegionDetailState(error = message)
            }
        }
    }
}
