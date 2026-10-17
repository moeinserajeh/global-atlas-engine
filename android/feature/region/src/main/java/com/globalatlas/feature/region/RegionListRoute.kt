package com.globalatlas.feature.region

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.globalatlas.core.network.AtlasApi
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateListOf

@Composable
fun RegionListRoute(countryIso2: String, api: AtlasApi, onRegionSelected: (Long, String) -> Unit = { _, _ -> }) {
    var loading by remember(countryIso2) { mutableStateOf(true) }
    var error by remember(countryIso2) { mutableStateOf<String?>(null) }
    val regions = remember(countryIso2) { mutableStateListOf<JsonObject>() }
    val scope = rememberCoroutineScope()
    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val response = api.regions(countryIso2.uppercase(), 100, 0)
                regions.clear()
                response.forEach { if (it.isJsonObject) regions.add(it.asJsonObject) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "دریافت فهرست استان‌ها ناموفق بود."
            } finally { loading = false }
        }
    }
    LaunchedEffect(countryIso2) { load() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("استان‌ها و مناطق", style = MaterialTheme.typography.headlineSmall)
        when {
            loading -> CircularProgressIndicator()
            error != null -> { Text(error!!, color = MaterialTheme.colorScheme.error); TextButton(onClick = { load() }) { Text("تلاش دوباره") } }
            regions.isEmpty() -> Text("برای این کشور منطقه‌ای ثبت نشده است.")
            else -> LazyColumn {
                items(regions, key = { it.get("id")?.toString() ?: it.hashCode().toString() }) { item ->
                    val name = item.get("nameFa")?.takeUnless { it.isJsonNull }?.asString
                        ?: item.get("nameEn")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        TextButton(onClick = { onRegionSelected(item.get("id")?.asLong ?: -1L, name) }) { Text(name) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
