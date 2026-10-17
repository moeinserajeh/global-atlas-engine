package com.globalatlas

import android.graphics.Color
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.JsonParser
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.expressions.Expression
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

@Composable
fun AtlasMapScreen(
    modifier: Modifier = Modifier,
    apiBaseUrl: String = BuildConfig.ATLAS_BASE_URL,
    onCountrySelected: (iso2: String, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var selectedName by remember { mutableStateOf("برای انتخاب کشور روی نقشه بزنید") }
    var loading by remember { mutableStateOf(true) }
    var mapError by remember { mutableStateOf<String?>(null) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { style ->
                    val source = RasterSource(
                        "osm-raster",
                        TileSet("osm-raster-tiles", "https://tile.openstreetmap.org/{z}/{x}/{y}.png"),
                        256
                    )
                    style.addSource(source)
                    style.addLayer(org.maplibre.android.style.layers.RasterLayer("osm-raster-layer", "osm-raster").withProperties(rasterOpacity(0.8f)))
                    thread(name = "atlas-geojson-loader") {
                        try {
                            val connection = URL(apiBaseUrl.trimEnd('/') + "/api/v1/map/countries").openConnection() as HttpURLConnection
                            connection.connectTimeout = 15000
                            connection.readTimeout = 30000
                            val json = connection.inputStream.bufferedReader().use { it.readText() }
                            val sourceGeoJson = GeoJsonSource("country-boundaries", json)
                            map.post {
                                map.style?.let { currentStyle ->
                                    currentStyle.addSource(sourceGeoJson)
                                    currentStyle.addLayer(FillLayer("country-fill", "country-boundaries").withProperties(
                                        fillColor(Expression.rgb(58, 125, 180)),
                                        fillOpacity(0.24f),
                                        fillOutlineColor(Expression.rgb(20, 60, 90))
                                    ))
                                    currentStyle.addLayer(LineLayer("country-lines", "country-boundaries").withProperties(
                                        lineColor(Expression.rgb(20, 60, 90)),
                                        lineWidth(0.8f)
                                    ))
                                }
                                loading = false
                            }
                        } catch (e: Exception) {
                            mapError = "مرزهای کشورها دریافت نشد؛ ابتدا داده‌ها را وارد و بک‌اند را اجرا کنید."
                            loading = false
                        }
                    }
                    map.addOnMapClickListener { point ->
                        val pixel = map.projection.toScreenLocation(point)
                        val features = map.queryRenderedFeatures(pixel, "country-fill")
                        if (features.isNotEmpty()) {
                            val properties = features[0].properties()
                            val iso2 = properties?.get("iso2")?.asString ?: ""
                            val name = properties?.get("nameFa")?.takeIf { !it.isJsonNull }?.asString
                                ?: properties?.get("nameEn")?.asString.orEmpty()
                            selectedName = name
                            if (iso2.isNotBlank()) onCountrySelected(iso2, name)
                            true
                        } else false
                    }
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(20.0, 0.0))
                        .zoom(1.2)
                        .build()
                }
            }
        }
    }
    DisposableEffect(mapView) {
        onDispose { mapView.onDestroy() }
    }
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { }
        )
        Surface(
            modifier = Modifier.align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ) {
            Text(selectedName, style = MaterialTheme.typography.titleMedium)
        }
        if (loading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        if (mapError != null) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter)) {
                Text(mapError!!, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
