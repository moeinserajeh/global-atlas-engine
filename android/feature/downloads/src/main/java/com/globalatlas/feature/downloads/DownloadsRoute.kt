package com.globalatlas.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DownloadsState(
    val isWorking: Boolean = false,
    val status: String? = null,
    val error: String? = null
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val store: OfflineAtlasStore,
    private val api: AtlasApi
) : ViewModel() {
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
                            val iso2 = item.stringOrNull("iso2")?.uppercase().orEmpty()
                            val nameEn = item.stringOrNull("nameEn").orEmpty()
                            if (iso2.length == 2 && nameEn.isNotBlank()) {
                                all += CountryEntity(
                                    iso2 = iso2,
                                    iso3 = item.stringOrNull("iso3"),
                                    nameEn = nameEn,
                                    nameFa = item.stringOrNull("nameFa"),
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
                _state.value = DownloadsState(error = e.toUserMessage("دریافت کشورها ناموفق بود."))
            }
        }
    }

    fun downloadRegions(countryIso2: String) {
        val iso2 = countryIso2.trim().uppercase()
        if (iso2.length != 2 || _state.value.isWorking) return
        viewModelScope.launch {
            _state.value = DownloadsState(isWorking = true, status = "در حال دریافت اطلاعات و هندسه ${iso2}…")
            try {
                val summaries = mutableListOf<AdministrativeRegionEntity>()
                var offset = 0
                while (true) {
                    val page = api.regions(iso2, 100, offset)
                    if (page.size() == 0) break
                    page.forEach { element ->
                        if (element.isJsonObject) {
                            val item = element.asJsonObject
                            val id = item.longOrNull("id")
                            val nameEn = item.stringOrNull("nameEn").orEmpty()
                            if (id != null && nameEn.isNotBlank()) {
                                summaries += AdministrativeRegionEntity(
                                    id = id,
                                    countryIso2 = iso2,
                                    nameEn = nameEn,
                                    nameFa = item.stringOrNull("nameFa"),
                                    level = item.intOrNull("adminLevel") ?: 1,
                                    geometryJson = null,
                                    cachedAtEpochMillis = System.currentTimeMillis()
                                )
                            }
                        }
                    }
                    offset += page.size()
                    if (page.size() < 100) break
                }

                val geometryResponse = api.regionGeometry(iso2)
                val geometryById = mutableMapOf<Long, String>()
                geometryResponse.getAsJsonArray("features")?.forEach { featureElement ->
                    if (!featureElement.isJsonObject) return@forEach
                    val feature = featureElement.asJsonObject
                    val properties = feature.getAsJsonObject("properties")
                    val id = properties?.longOrNull("id") ?: feature.longOrNull("id") ?: return@forEach
                    val geometry = feature.get("geometry")?.takeUnless { it.isJsonNull } ?: return@forEach
                    geometryById[id] = geometry.toString()
                }

                val cached = summaries.map { region ->
                    region.copy(
                        geometryJson = geometryById[region.id],
                        cachedAtEpochMillis = System.currentTimeMillis()
                    )
                }
                store.replaceCountryRegions(iso2, cached)
                val geometryCount = cached.count { !it.geometryJson.isNullOrBlank() }
                _state.value = DownloadsState(
                    status = "ذخیره شد: ${cached.size} منطقه، هندسهٔ ${geometryCount} منطقه برای ${iso2}"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = DownloadsState(error = e.toUserMessage("دریافت استان‌ها و هندسه ناموفق بود."))
            }
        }
    }
}

private fun com.google.gson.JsonObject.stringOrNull(name: String): String? =
    get(name)?.takeUnless { it.isJsonNull }?.asString

private fun com.google.gson.JsonObject.longOrNull(name: String): Long? =
    get(name)?.takeUnless { it.isJsonNull }?.asLong

private fun com.google.gson.JsonObject.intOrNull(name: String): Int? =
    get(name)?.takeUnless { it.isJsonNull }?.asInt

private fun Exception.toUserMessage(fallback: String): String =
    if (this is HttpException) "خطای سرور: ${code()}" else fallback

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
                    Button(onClick = { viewModel.downloadRegions(country.iso2) }, enabled = !state.isWorking) {
                        Text("دانلود مناطق و هندسه")
                    }
                }
            }
        }
        Text("بازدیدهای اخیر: ${recent.size}", style = MaterialTheme.typography.titleMedium)
        if (recent.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(0.5f)) {
                items(recent, key = { it.entityType + ":" + it.entityId }) { item ->
                    Text(item.displayName, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
