package com.globalatlas.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.globalatlas.core.database.AdministrativeRegionEntity
import com.globalatlas.core.database.CountryEntity
import com.globalatlas.core.database.OfflineAtlasStore
import com.globalatlas.core.network.AtlasApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DownloadsState(val isWorking: Boolean = false, val status: String? = null, val error: String? = null)

@HiltViewModel
class DownloadsViewModel @Inject constructor(private val store: OfflineAtlasStore, private val api: AtlasApi) : ViewModel() {
    val countries = store.observeCountries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recent = store.observeRecent(100).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _state = MutableStateFlow(DownloadsState())
    val state = _state.asStateFlow()

    fun downloadCountries() {
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.value = DownloadsState(isWorking = true, status = "در حال دریافت فهرست کشورها…")
            try {
                val all = mutableListOf<CountryEntity>()
                var offset = 0
                while (true) {
                    val page = api.countries(limit = 100, offset = offset)
                    if (page.size() == 0) break
                    page.forEach { element ->
                        if (element.isJsonObject) {
                            val item = element.asJsonObject
                            val iso2 = item.get("iso2")?.takeUnless { it.isJsonNull }?.asString?.uppercase().orEmpty()
                            val nameEn = item.get("nameEn")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                            if (iso2.length == 2 && nameEn.isNotBlank()) {
                                all += CountryEntity(
                                    iso2 = iso2,
                                    iso3 = item.get("iso3")?.takeUnless { it.isJsonNull }?.asString,
                                    nameEn = nameEn,
                                    nameFa = item.get("nameFa")?.takeUnless { it.isJsonNull }?.asString,
                                    cachedAtEpochMillis = System.currentTimeMillis()
                                )
                            }
                        }
                    }
                    offset += page.size()
                    if (page.size() < 100) break
                }
                store.cacheCountries(all)
                _state.value = DownloadsState(status = "ذخیره شد: ${all.size} کشور")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = DownloadsState(error = if (e is HttpException) "خطای سرور: ${e.code()}" else "دریافت کشورها ناموفق بود.")
            }
        }
    }

    fun downloadRegions(countryIso2: String) {
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.value = DownloadsState(isWorking = true, status = "در حال دریافت استان‌های ${countryIso2.uppercase()}…")
            try {
                val all = mutableListOf<AdministrativeRegionEntity>()
                var offset = 0
                while (true) {
                    val page = api.regions(countryIso2.uppercase(), 100, offset)
                    if (page.size() == 0) break
                    page.forEach { element ->
                        if (element.isJsonObject) {
                            val item = element.asJsonObject
                            val id = item.get("id")?.takeUnless { it.isJsonNull }?.asLong
                            val nameEn = item.get("nameEn")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                            if (id != null && nameEn.isNotBlank()) all += AdministrativeRegionEntity(
                                id = id,
                                countryIso2 = countryIso2.uppercase(),
                                nameEn = nameEn,
                                nameFa = item.get("nameFa")?.takeUnless { it.isJsonNull }?.asString,
                                level = item.get("adminLevel")?.takeUnless { it.isJsonNull }?.asInt ?: 1,
                                geometryJson = null,
                                cachedAtEpochMillis = System.currentTimeMillis()
                            )
                        }
                    }
                    offset += page.size()
                    if (page.size() < 100) break
                }
                store.replaceCountryRegions(countryIso2, all)
                _state.value = DownloadsState(status = "ذخیره شد: ${all.size} استان/منطقه برای ${countryIso2.uppercase()}")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = DownloadsState(error = if (e is HttpException) "خطای سرور: ${e.code()}" else "دریافت استان‌ها ناموفق بود.")
            }
        }
    }
}

@Composable
fun DownloadsRoute(modifier: Modifier = Modifier, viewModel: DownloadsViewModel = hiltViewModel()) {
    val countries by viewModel.countries.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val state by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داده‌های آفلاین", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { viewModel.downloadCountries() }, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
            Text("دانلود فهرست همهٔ کشورها")
        }
        if (state.isWorking) CircularProgressIndicator()
        state.status?.let { Text(it) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("کشورهای ذخیره‌شده: ${countries.size}")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(countries, key = { it.iso2 }) { country ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(country.nameFa ?: country.nameEn)
                        Text(country.iso2, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { viewModel.downloadRegions(country.iso2) }, enabled = !state.isWorking) { Text("دانلود استان‌ها") }
                }
            }
        }
        Text("بازدیدهای اخیر: ${recent.size}", style = MaterialTheme.typography.titleMedium)
        if (recent.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(0.5f)) {
                items(recent, key = { it.entityType + ":" + it.entityId }) { item -> Text(item.displayName, modifier = Modifier.padding(vertical = 4.dp)) }
            }
        }
    }
}
