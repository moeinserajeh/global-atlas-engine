package com.globalatlas.feature.region

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegionListRoute(countryIso2: String, onRegionSelected: (Long, String) -> Unit = { _, _ -> }, modifier: Modifier = Modifier, viewModel: RegionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(countryIso2) { viewModel.load(countryIso2) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.regions_title, countryIso2.uppercase()), style = MaterialTheme.typography.headlineSmall)
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> { Text(state.error!!, color = MaterialTheme.colorScheme.error); TextButton(onClick = { viewModel.load(countryIso2, retry = true) }) { Text(stringResource(R.string.retry)) } }
            state.regions.isEmpty() -> Text(stringResource(R.string.no_regions))
            else -> LazyColumn {
                items(state.regions, key = { it.get("id")?.toString() ?: it.hashCode().toString() }) { item ->
                    val name = item.get("nameFa")?.takeUnless { it.isJsonNull }?.asString ?: item.get("nameEn")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                    Column(Modifier.fillMaxWidth().clickable { onRegionSelected(item.get("id")?.asLong ?: -1L, name) }.padding(vertical = 8.dp)) { Text(name); HorizontalDivider() }
                }
            }
        }
    }
}
