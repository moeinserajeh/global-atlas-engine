package com.globalatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.globalatlas.core.designsystem.AtlasTheme
import com.globalatlas.feature.atlas.AtlasRoute
import com.globalatlas.feature.country.CountryDetailRoute
import com.globalatlas.feature.downloads.DownloadsRoute
import com.globalatlas.feature.region.RegionListRoute
import com.globalatlas.feature.search.SearchRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AtlasTheme { AtlasNavigation() } }
    }
}

@Composable
private fun AtlasNavigation() {
    val nav = rememberNavController()
    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { nav.navigate("atlas") { launchSingleTop = true } }, modifier = Modifier.weight(1f)) { Text("نقشه") }
                Button(onClick = { nav.navigate("search") { launchSingleTop = true } }, modifier = Modifier.weight(1f)) { Text("جست‌وجو") }
                Button(onClick = { nav.navigate("downloads") { launchSingleTop = true } }, modifier = Modifier.weight(1f)) { Text("آفلاین") }
            }
        }
        NavHost(navController = nav, startDestination = "atlas", modifier = Modifier.weight(1f)) {
            composable("atlas") {
                AtlasRoute(
                    onCountrySelected = { iso2, _ -> nav.navigate("country/${iso2.uppercase()}") },
                    onRegionSelected = { id, name -> nav.navigate("region-detail/$id/${android.net.Uri.encode(name)}") }
                )
            }
            composable("search") {
                SearchRoute(
                    onCountrySelected = { iso2, _ -> nav.navigate("country/${iso2.uppercase()}") },
                    onRegionSelected = { id, name -> nav.navigate("region-detail/$id/${android.net.Uri.encode(name)}") }
                )
            }
            composable("downloads") { DownloadsRoute() }
            composable("country/{iso2}", arguments = listOf(navArgument("iso2") { type = NavType.StringType })) { entry ->
                val iso2 = entry.arguments?.getString("iso2").orEmpty()
                Column {
                    Button(onClick = { nav.navigate("regions/$iso2") }) { Text("نمایش استان‌ها") }
                    CountryDetailRoute(iso2 = iso2)
                }
            }
            composable("regions/{iso2}", arguments = listOf(navArgument("iso2") { type = NavType.StringType })) { entry ->
                RegionListRoute(
                    countryIso2 = entry.arguments?.getString("iso2").orEmpty(),
                    onRegionSelected = { id, name -> nav.navigate("region-detail/$id/${android.net.Uri.encode(name)}") }
                )
            }
            composable(
                "region-detail/{id}/{name}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("name") { type = NavType.StringType })
            ) { entry ->
                RegionDetailPlaceholder(
                    id = entry.arguments?.getLong("id") ?: -1L,
                    name = entry.arguments?.getString("name").orEmpty()
                )
            }
        }
    }
}

@Composable
private fun RegionDetailPlaceholder(id: Long, name: String) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("جزئیات منطقه", style = MaterialTheme.typography.headlineSmall)
        Text(name, style = MaterialTheme.typography.titleLarge)
        Text("شناسه: $id")
        Text("جزئیات تکمیلی این منطقه هنوز از API جزئیات دریافت نمی‌شود.")
    }
}
