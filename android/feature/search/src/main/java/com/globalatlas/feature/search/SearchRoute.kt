package com.globalatlas.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    onCountrySelected: (String, String) -> Unit = { _, _ -> },
    onRegionSelected: (Long, String) -> Unit = { _, _ -> },
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.search_title), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.dispatch(SearchIntent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.search_hint)) },
            trailingIcon = { if (state.query.isNotEmpty()) TextButton(onClick = { viewModel.dispatch(SearchIntent.Clear) }) { Text(stringResource(R.string.clear)) } }
        )
        if (state.isLoading) CircularProgressIndicator()
        state.error?.let { Row { Text(it, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error); TextButton(onClick = { viewModel.dispatch(SearchIntent.Retry) }) { Text(stringResource(R.string.retry)) } } }
        if (state.query.trim().length >= 2 && !state.isLoading && state.error == null && state.results.isEmpty()) Text(stringResource(R.string.no_results))
        LazyColumn {
            items(state.results, key = { it.get("_type")?.asString.orEmpty() + ":" + (it.get("id")?.toString() ?: it.get("iso2")?.asString.orEmpty()) }) { item ->
                val type = item.get("_type")?.asString.orEmpty()
                val title = item.get("nameFa")?.takeUnless { it.isJsonNull }?.asString
                    ?: item.get("nameEn")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                val nameEn = item.get("nameEn")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                val subtitle = if (type == "REGION") {
                    val countryFa = item.get("countryNameFa")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                    val countryEn = item.get("countryNameEn")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                    listOf(countryFa, countryEn).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                } else {
                    val iso2 = item.get("iso2")?.asString.orEmpty()
                    val iso3 = item.get("iso3")?.asString.orEmpty()
                    listOf(iso2, iso3, nameEn).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                }
                Column(Modifier.fillMaxWidth().clickable {
                    if (type == "COUNTRY") onCountrySelected(item.get("iso2")?.asString.orEmpty(), title)
                    else onRegionSelected(item.get("id")?.asLong ?: -1L, title)
                }.padding(vertical = 8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (nameEn.isNotBlank() && nameEn != title) Text(nameEn, style = MaterialTheme.typography.bodyMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
            }
        }
    }
}
