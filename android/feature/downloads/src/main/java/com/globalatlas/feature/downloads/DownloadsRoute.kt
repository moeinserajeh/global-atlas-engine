package com.globalatlas.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.globalatlas.core.database.OfflineAtlasStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DownloadsViewModel @Inject constructor(store: OfflineAtlasStore) : ViewModel() {
    val countries = store.observeCountries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val regions = store.observeRecent(100).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun DownloadsRoute(modifier: Modifier = Modifier, viewModel: DownloadsViewModel = hiltViewModel()) {
    val countries by viewModel.countries.collectAsState()
    val recent by viewModel.regions.collectAsState()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("داده‌های آفلاین", style = MaterialTheme.typography.headlineSmall)
        Text("کشورهای ذخیره‌شده: ${countries.size}")
        if (countries.isNotEmpty()) {
            LazyColumn {
                items(countries, key = { it.iso2 }) { country ->
                    Text(country.nameFa ?: country.nameEn, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        } else Text("هنوز کشوری در حافظهٔ آفلاین ذخیره نشده است.")
        Text("بازدیدهای اخیر: ${recent.size}", style = MaterialTheme.typography.titleMedium)
        if (recent.isNotEmpty()) {
            LazyColumn {
                items(recent, key = { it.type + ":" + it.itemId }) { item ->
                    Text(item.displayName, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
