package com.globalatlas.feature.atlas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.globalatlas.core.designsystem.AtlasTheme
@Composable fun AtlasRoute(viewModel: AtlasViewModel = hiltViewModel()) {
 val state by viewModel.state.collectAsState()
 LaunchedEffect(Unit) { viewModel.dispatch(AtlasIntent.Load) }
 AtlasTheme {
  Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
   Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("اطلس جهان‌نما", style = MaterialTheme.typography.headlineMedium)
    Text("نقشه تعاملی کشورها و تقسیمات اداری", style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(value = state.searchQuery, onValueChange = { viewModel.dispatch(AtlasIntent.SearchChanged(it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("جست‌وجوی کشور یا استان") }, singleLine = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("وضعیت: آماده"); state.selectedCountryName?.let { Text("کشور: $it") } }
    Text("ماژول اطلس به معماری MVI منتقل شده است. نمایش کامل مرزها پس از اتصال لایه نقشه و داده‌های بک‌اند فعال می‌شود.", style = MaterialTheme.typography.bodyMedium)
   }
  }
 }
}