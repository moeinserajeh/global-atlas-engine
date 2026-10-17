package com.globalatlas.feature.atlas
import com.google.gson.JsonObject
sealed interface AtlasIntent {
 data object Load:AtlasIntent
 data class SearchChanged(val query:String):AtlasIntent
 data class CountrySelected(val iso2:String,val name:String):AtlasIntent
 data class RegionSelected(val id:Long,val name:String):AtlasIntent
 data object ClearSearch:AtlasIntent
 data object RetrySearch:AtlasIntent
}
data class AtlasState(val isLoading:Boolean=false,val searchQuery:String="",val searchResults:List<JsonObject> = emptyList(),val isSearching:Boolean=false,val searchError:String?=null,val selectedCountryIso2:String?=null,val selectedCountryName:String?=null,val selectedRegionName:String?=null,val message:String?=null)
sealed interface AtlasEffect { data class OpenCountry(val iso2:String):AtlasEffect; data class OpenRegion(val id:Long):AtlasEffect; data class ShowMessage(val message:String):AtlasEffect }
