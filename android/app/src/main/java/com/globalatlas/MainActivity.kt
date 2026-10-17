package com.globalatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

data class CountryUiModel(
    val id: Long,
    val iso2: String,
    val iso3: String,
    @SerializedName("nameEn") val nameEn: String,
    @SerializedName("nameFa") val nameFa: String?
)

interface AtlasApi {
    @GET("api/v1/countries")
    suspend fun getCountries(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): List<CountryUiModel>
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAtlasApi(): AtlasApi = Retrofit.Builder()
        .baseUrl(BuildConfig.ATLAS_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AtlasApi::class.java)
}

data class AtlasUiState(
    val isLoading: Boolean = true,
    val countries: List<CountryUiModel> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AtlasViewModel @Inject constructor(
    private val api: AtlasApi
) : ViewModel() {
    private val _uiState = MutableStateFlow(AtlasUiState())
    val uiState = _uiState.asStateFlow()

    init { loadCountries() }

    fun loadCountries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { api.getCountries() }
                .onSuccess { _uiState.value = AtlasUiState(isLoading = false, countries = it) }
                .onFailure {
                    _uiState.value = AtlasUiState(
                        isLoading = false,
                        error = "دریافت کشورها ناموفق بود. اتصال سرور و آدرس API را بررسی کن."
                    )
                }
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: AtlasViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsState()
                var query by remember { mutableStateOf("") }
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Text("اطلس جهان‌نما", style = MaterialTheme.typography.headlineMedium)
                        Text("کشورها", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("جست‌وجو در کشورهای دریافت‌شده") },
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        when {
                            state.isLoading -> CircularProgressIndicator()
                            state.error != null -> Text(state.error!!)
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.countries.filter {
                                    query.isBlank() ||
                                        it.nameEn.contains(query, ignoreCase = true) ||
                                        (it.nameFa?.contains(query, ignoreCase = true) == true) ||
                                        it.iso2.contains(query, ignoreCase = true)
                                }, key = { it.id }) { country ->
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(country.nameFa?.takeIf { it.isNotBlank() } ?: country.nameEn)
                                        Text("${country.nameEn} · ${country.iso2}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
