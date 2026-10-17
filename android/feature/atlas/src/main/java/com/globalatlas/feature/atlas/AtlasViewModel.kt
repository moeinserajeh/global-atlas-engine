package com.globalatlas.feature.atlas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.globalatlas.core.network.AtlasApi
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class AtlasViewModel @Inject constructor(private val api:AtlasApi):ViewModel() {
 private val _state=MutableStateFlow(AtlasState())
 val state=_state.asStateFlow()
 private val _effects=MutableSharedFlow<AtlasEffect>()
 val effects=_effects.asSharedFlow()
 private var searchJob:Job?=null
 fun dispatch(intent:AtlasIntent) {
  when(intent) {
   AtlasIntent.Load -> Unit
   is AtlasIntent.SearchChanged -> {
    _state.update { it.copy(searchQuery=intent.query,searchError=null) }
    searchJob?.cancel()
    val q=intent.query.trim()
    if(q.length<2) _state.update { it.copy(searchResults=emptyList(),isSearching=false) }
    else searchJob=viewModelScope.launch { delay(350); runSearch(q) }
   }
   AtlasIntent.ClearSearch -> { searchJob?.cancel(); _state.update { it.copy(searchQuery="",searchResults=emptyList(),searchError=null,isSearching=false) } }
   AtlasIntent.RetrySearch -> { val q=state.value.searchQuery.trim(); if(q.length>=2) { searchJob?.cancel(); searchJob=viewModelScope.launch { runSearch(q) } } }
   is AtlasIntent.CountrySelected -> _state.update { it.copy(selectedCountryIso2=intent.iso2,selectedCountryName=intent.name,selectedRegionName=null) }
   is AtlasIntent.RegionSelected -> _state.update { it.copy(selectedRegionName=intent.name) }
  }
 }
 private suspend fun runSearch(query:String) {
  _state.update { it.copy(isSearching=true,searchError=null) }
  try {
   val countries=api.searchCountries(query,15)
   val regions=api.searchRegions(query,15)
   val combined=buildList {
    countries.forEach { add(it.asJsonObject.apply { addProperty("_type","COUNTRY") }) }
    regions.forEach { add(it.asJsonObject.apply { addProperty("_type","REGION") }) }
   }.take(25)
   _state.update { it.copy(searchResults=combined,isSearching=false) }
  } catch(e:Exception) {
   if(e is CancellationException) throw e
   val message=if(e is HttpException) "خطای سرور ("+e.code()+")" else "اتصال به سرور برقرار نشد؛ آدرس API و وضعیت بک‌اند را بررسی کنید."
   _state.update { it.copy(isSearching=false,searchError=message) }
  }
 }
}
