package com.globalatlas.feature.atlas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AtlasRoute(onCountrySelected: (String, String) -> Unit = { _, _ -> }, onRegionSelected: (Long, String) -> Unit = { _, _ -> }, viewModel:AtlasViewModel=hiltViewModel()) {
 val state by viewModel.state.collectAsState()
 LaunchedEffect(Unit) { viewModel.dispatch(AtlasIntent.Load) }
 Box {
  AtlasMapScreen(onCountrySelected={ iso2,name -> viewModel.dispatch(AtlasIntent.CountrySelected(iso2,name)); onCountrySelected(iso2,name) })
  Surface(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),shape=MaterialTheme.shapes.large,tonalElevation=4.dp) {
   Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(value=state.searchQuery,onValueChange={viewModel.dispatch(AtlasIntent.SearchChanged(it))},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("جست‌وجوی کشور یا استان")},trailingIcon={if(state.searchQuery.isNotEmpty()) TextButton(onClick={viewModel.dispatch(AtlasIntent.ClearSearch)}) { Text("پاک") }})
    if(state.isSearching) CircularProgressIndicator()
    state.searchError?.let { Row(verticalAlignment=Alignment.CenterVertically) { Text(it,Modifier.weight(1f),color=MaterialTheme.colorScheme.error); TextButton(onClick={viewModel.dispatch(AtlasIntent.RetrySearch)}) { Text("تلاش دوباره") } } }
    if(state.searchQuery.trim().length>=2 && !state.isSearching && state.searchError==null && state.searchResults.isEmpty()) Text("نتیجه‌ای پیدا نشد")
    if(state.searchResults.isNotEmpty()) LazyColumn {
     items(state.searchResults,key={it.get("_type").asString+":"+(it.get("id")?.toString()?:it.get("iso2")?.asString.orEmpty())}) { item ->
      val type=item.get("_type")?.asString.orEmpty()
      val title=item.get("nameFa")?.takeUnless{it.isJsonNull}?.asString?:item.get("nameEn")?.takeUnless{it.isJsonNull}?.asString.orEmpty()
      val subtitle=if(type=="REGION") item.get("countryNameFa")?.takeUnless{it.isJsonNull}?.asString?:item.get("countryNameEn")?.takeUnless{it.isJsonNull}?.asString.orEmpty() else item.get("iso2")?.asString.orEmpty()
      Column(Modifier.fillMaxWidth().clickable {
       if(type=="COUNTRY") { val iso2=item.get("iso2")?.asString.orEmpty(); viewModel.dispatch(AtlasIntent.CountrySelected(iso2,title)); onCountrySelected(iso2,title) }
       else { val id=item.get("id")?.asLong?:-1L; viewModel.dispatch(AtlasIntent.RegionSelected(id,title)); onRegionSelected(id,title) }
       viewModel.dispatch(AtlasIntent.ClearSearch)
      }.padding(vertical=8.dp)) { Text(title,style=MaterialTheme.typography.titleSmall); Text(subtitle,style=MaterialTheme.typography.bodySmall) }
     }
    }
   }
  }
 }
}
