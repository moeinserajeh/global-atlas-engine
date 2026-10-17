package com.globalatlas.feature.country

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CountryDetailRoute(
    iso2: String,
    modifier: Modifier = Modifier,
    onShowRegions: (String) -> Unit = {},
    viewModel: CountryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(iso2) { viewModel.dispatch(CountryDetailIntent.Load(iso2)) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.country_profile), style = MaterialTheme.typography.headlineSmall)
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.dispatch(CountryDetailIntent.Retry) }) { Text(stringResource(R.string.retry)) }
            }
            state.country != null -> {
                val item = state.country!!
                val nameFa = item.stringValue("nameFa")
                val nameEn = item.stringValue("nameEn")
                Text(nameFa.ifBlank { nameEn }, style = MaterialTheme.typography.headlineMedium)
                if (nameFa.isNotBlank() && nameEn.isNotBlank() && nameFa != nameEn) {
                    Text(nameEn, style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoCard("ISO-2", item.stringValue("iso2"))
                    InfoCard("ISO-3", item.stringValue("iso3"))
                }
                InfoCard(stringResource(R.string.country_capital), item.stringValue("capital").ifBlank { stringResource(R.string.country_not_recorded) })
                InfoCard(stringResource(R.string.country_population), item.longValue("population")?.let(::formatNumber) ?: "اطلاعات ثبت نشده")
                InfoCard(stringResource(R.string.country_area), item.decimalValue("areaKm2")?.let(::formatNumber) ?: "اطلاعات ثبت نشده")
                InfoCard(stringResource(R.string.country_admin_regions), item.longValue("regionCount")?.let(::formatNumber) ?: "—")
                Button(onClick = { onShowRegions(item.stringValue("iso2")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.country_show_regions))
                }
                Text(stringResource(R.string.country_sources_version), style = MaterialTheme.typography.titleMedium)
                InfoCard(stringResource(R.string.source), item.stringValue("dataSource").ifBlank { stringResource(R.string.country_not_recorded) })
                InfoCard(stringResource(R.string.license), item.stringValue("dataLicense").ifBlank { "ثبت نشده" })
                InfoCard(stringResource(R.string.dataset_version), item.stringValue("datasetVersion").ifBlank { stringResource(R.string.country_not_recorded) })
                InfoCard(stringResource(R.string.reference_date), item.stringValue("referenceDate").ifBlank { "نامشخص" })
            }
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun com.google.gson.JsonObject.stringValue(key: String): String =
    get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()

private fun com.google.gson.JsonObject.longValue(key: String): Long? =
    get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asLong }.getOrNull() }

private fun com.google.gson.JsonObject.decimalValue(key: String): Double? =
    get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asDouble }.getOrNull() }

private fun formatNumber(value: Number): String =
    java.text.NumberFormat.getIntegerInstance(java.util.Locale("fa", "IR")).format(value)
