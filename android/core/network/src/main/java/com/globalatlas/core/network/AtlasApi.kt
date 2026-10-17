package com.globalatlas.core.network
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
interface AtlasApi {
 @GET("api/v1/countries") suspend fun countries(@Query("limit") limit:Int=100,@Query("offset") offset:Int=0):JsonArray
 @GET("api/v1/countries/search") suspend fun searchCountries(@Query("q") query:String,@Query("limit") limit:Int=30):JsonArray
 @GET("api/v1/countries/{iso2}") suspend fun country(@Path("iso2") iso2:String):JsonObject
 @GET("api/v1/countries/{iso2}/regions") suspend fun regions(@Path("iso2") iso2:String,@Query("limit") limit:Int=100,@Query("offset") offset:Int=0):JsonArray
 @GET("api/v1/regions/search") suspend fun searchRegions(@Query("q") query:String,@Query("limit") limit:Int=30):JsonArray
}
