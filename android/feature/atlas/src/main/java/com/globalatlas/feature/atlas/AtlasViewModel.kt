package com.globalatlas.feature.atlas
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
@HiltViewModel
class AtlasViewModel @Inject constructor() : ViewModel() {
 private val _state = MutableStateFlow(AtlasState())
 val state = _state.asStateFlow()
 private val _effects = MutableSharedFlow<AtlasEffect>()
 val effects = _effects.asSharedFlow()
 fun dispatch(intent: AtlasIntent) {
  when(intent) {
   AtlasIntent.Load -> _state.update { it.copy(isLoading = false) }
   is AtlasIntent.SearchChanged -> _state.update { it.copy(searchQuery = intent.query) }
   is AtlasIntent.CountrySelected -> _state.update { it.copy(selectedCountryIso2 = intent.iso2, selectedCountryName = intent.name, selectedRegionName = null) }
   is AtlasIntent.RegionSelected -> _state.update { it.copy(selectedRegionName = intent.name) }
  }
 }
}