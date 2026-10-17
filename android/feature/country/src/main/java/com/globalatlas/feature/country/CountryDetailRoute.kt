package com.globalatlas.feature.country

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CountryDetailRoute(iso2: String, modifier: Modifier = Modifier, viewModel: CountryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(iso2) { viewModel.dispatch(CountryDetailIntent.Load(iso2)) }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("جزئیات کشور", style = MaterialTheme.typography.headlineSmall)
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.dispatch(CountryDetailIntent.Retry) }) { Text("تلاش دوباره") }
            }
            state.country != null -> {
                val item = state.country!!
                Text(item.get("nameFa")?.takeUnless { it.isJsonNull }?.asString
                    ?: item.get("nameEn")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                    style = MaterialTheme.typography.titleLarge)
                Text("کد: ${item.get("iso2")?.asString.orEmpty()}")
                Text("پایتخت: ${item.get("capital")?.takeUnless { it.isJsonNull }?.asString ?: "ثبت نشده"}")
                Text("جمعیت: ${item.get("population")?.takeUnless { it.isJsonNull }?.toString() ?: "ثبت نشده"}")
            }
        }
    }
}
