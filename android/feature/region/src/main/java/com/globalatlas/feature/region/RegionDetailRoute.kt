package com.globalatlas.feature.region

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
fun RegionDetailRoute(id: Long, fallbackName: String, modifier: Modifier = Modifier, viewModel: RegionDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(id) { viewModel.load(id) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.region_details), style = MaterialTheme.typography.headlineSmall)
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.load(id, retry = true) }) { Text(stringResource(R.string.retry)) }
                Text(fallbackName)
            }
            state.detail != null -> {
                val item = state.detail!!
                val nameFa = item.stringValue("nameFa")
                val nameEn = item.stringValue("nameEn")
                Text(nameFa.ifBlank { nameEn.ifBlank { fallbackName } }, style = MaterialTheme.typography.headlineMedium)
                if (nameFa.isNotBlank() && nameEn.isNotBlank() && nameFa != nameEn) Text(nameEn, style = MaterialTheme.typography.titleMedium)
                DetailValue(stringResource(R.string.country), item.stringValue("countryNameFa").ifBlank { item.stringValue("countryNameEn") })
                DetailValue(stringResource(R.string.country_code), item.stringValue("countryIso2"))
                DetailValue(stringResource(R.string.region_code), item.stringValue("code").ifBlank { stringResource(R.string.not_recorded) })
                DetailValue(stringResource(R.string.admin_level), item.get("adminLevel")?.toString() ?: "—")
                DetailValue(stringResource(R.string.approx_area), item.get("areaKm2")?.toString() ?: "—")
                DetailValue(stringResource(R.string.geometry_validity), stringResource(if (item.get("geometryValid")?.asBoolean == true) R.string.valid else R.string.unknown_invalid))
                Text(stringResource(R.string.source_version), style = MaterialTheme.typography.titleMedium)
                DetailValue(stringResource(R.string.source), item.stringValue("dataSource").ifBlank { stringResource(R.string.not_recorded) })
                DetailValue(stringResource(R.string.license), item.stringValue("dataLicense").ifBlank { "ثبت نشده" })
                DetailValue(stringResource(R.string.version), item.stringValue("datasetVersion").ifBlank { stringResource(R.string.unknown) })
                DetailValue(stringResource(R.string.reference_date), item.stringValue("referenceDate").ifBlank { "نامشخص" })
            }
        }
    }
}
@Composable
private fun DetailValue(label: String, value: String) {
    Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    } }
}
private fun com.google.gson.JsonObject.stringValue(key: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
