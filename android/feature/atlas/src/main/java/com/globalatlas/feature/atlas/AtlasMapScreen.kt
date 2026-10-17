package com.globalatlas.feature.atlas

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonElement
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

private const val COUNTRY_SOURCE = "country-boundaries"
private const val COUNTRY_FILL = "country-fill"
private const val COUNTRY_LINES = "country-lines"
private const val COUNTRY_SELECTED = "country-selected"
private const val REGION_SOURCE = "region-boundaries"
private const val REGION_FILL = "region-fill"
private const val REGION_LINES = "region-lines"
private const val REGION_SELECTED = "region-selected"

@Composable
fun AtlasMapScreen(
    modifier: Modifier = Modifier,
    apiBaseUrl: String = "http://10.0.2.2:8080/",
    onCountrySelected: (iso2: String, name: String) -> Unit = { _, _ -> },
    onRegionSelected: (id: Long, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var selectedIso2 by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var mapError by remember { mutableStateOf<String?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { mapLibreMap ->
                map = mapLibreMap
                mapLibreMap.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { style ->
                    style.addSource(
                        RasterSource(
                            "osm-raster",
                            TileSet("osm-raster-tiles", "https://tile.openstreetmap.org/{z}/{x}/{y}.png"),
                            256
                        )
                    )
                    style.addLayer(RasterLayer("osm-raster-layer", "osm-raster").withProperties(rasterOpacity(0.82f)))
                    mapLibreMap.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(20.0, 0.0))
                        .zoom(1.2)
                        .build()

                    thread(name = "atlas-country-geojson", isDaemon = true) {
                        try {
                            val connection = URL(apiBaseUrl.trimEnd('/') + "/api/v1/map/countries")
                                .openConnection() as HttpURLConnection
                            connection.connectTimeout = 15000
                            connection.readTimeout = 30000
                            val json = connection.inputStream.bufferedReader().use { it.readText() }
                            connection.disconnect()
                            mainHandler.post {
                                mapLibreMap.style?.let { currentStyle ->
                                    currentStyle.addSource(GeoJsonSource(COUNTRY_SOURCE, json))
                                    currentStyle.addLayer(
                                        FillLayer(COUNTRY_FILL, COUNTRY_SOURCE).withProperties(
                                            fillColor(Expression.rgb(58, 125, 180)),
                                            fillOpacity(0.24f),
                                            fillOutlineColor(Expression.rgb(20, 60, 90))
                                        )
                                    )
                                    currentStyle.addLayer(
                                        LineLayer(COUNTRY_LINES, COUNTRY_SOURCE).withProperties(
                                            lineColor(Expression.rgb(20, 60, 90)),
                                            lineWidth(0.8f)
                                        )
                                    )
                                    currentStyle.addLayer(
                                        FillLayer(COUNTRY_SELECTED, COUNTRY_SOURCE).withProperties(
                                            fillColor(Expression.rgb(38, 166, 154)),
                                            fillOpacity(0.58f),
                                            fillOutlineColor(Expression.rgb(0, 90, 80))
                                        ).apply { setFilter(Expression.eq(Expression.get("iso2"), Expression.literal("__none__"))) }
                                    )
                                }
                                loading = false
                            }
                        } catch (_: Exception) {
                            mainHandler.post {
                                mapError = context.getString(R.string.map_load_countries_error)
                                loading = false
                            }
                        }
                    }

                    mapLibreMap.addOnMapClickListener { latLng ->
                        val screenPoint = mapLibreMap.projection.toScreenLocation(latLng)
                        val activeStyle = mapLibreMap.style ?: return@addOnMapClickListener false

                        val regionFeatures = mapLibreMap.queryRenderedFeatures(screenPoint, REGION_FILL)
                        if (regionFeatures.isNotEmpty()) {
                            val props = regionFeatures.first().properties()
                            val name = props?.get("nameFa").safeJsonString()
                                .ifBlank { props?.get("nameEn").safeJsonString() }
                            val id = props?.get("id").safeJsonLong()
                            selectedName = name.ifBlank { context.getString(R.string.map_region_selected) }
                            if (id != null) {
                                (activeStyle.getLayer(REGION_SELECTED) as? FillLayer)?.setFilter(
                                    Expression.eq(Expression.get("id"), Expression.literal(id))
                                )
                                onRegionSelected(id, selectedName)
                            }
                            return@addOnMapClickListener true
                        }

                        val countries = mapLibreMap.queryRenderedFeatures(screenPoint, COUNTRY_FILL)
                        if (countries.isEmpty()) {
                            selectedIso2 = null
                            selectedName = context.getString(R.string.map_select_country)
                            (activeStyle.getLayer(COUNTRY_SELECTED) as? FillLayer)?.setFilter(
                                Expression.eq(Expression.get("iso2"), Expression.literal("__none__"))
                            )
                            clearRegionLayers(activeStyle)
                            return@addOnMapClickListener false
                        }

                        val props = countries.first().properties()
                        val iso2 = props?.get("iso2").safeJsonString().uppercase()
                        val name = props?.get("nameFa").safeJsonString()
                            .ifBlank { props?.get("nameEn").safeJsonString() }
                        if (iso2.length != 2) return@addOnMapClickListener true

                        selectedIso2 = iso2
                        selectedName = name.ifBlank { iso2 }
                        mapError = null
                        (activeStyle.getLayer(COUNTRY_SELECTED) as? FillLayer)?.setFilter(
                            Expression.eq(Expression.get("iso2"), Expression.literal(iso2))
                        )
                        clearRegionLayers(activeStyle)
                        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 3.5), 450)
                        onCountrySelected(iso2, selectedName)

                        thread(name = "atlas-region-geojson", isDaemon = true) {
                            try {
                                val connection = URL(
                                    apiBaseUrl.trimEnd('/') + "/api/v1/map/countries/" + iso2 + "/regions"
                                ).openConnection() as HttpURLConnection
                                connection.connectTimeout = 15000
                                connection.readTimeout = 30000
                                val json = connection.inputStream.bufferedReader().use { it.readText() }
                                connection.disconnect()
                                mainHandler.post {
                                    mapLibreMap.style?.let { currentStyle ->
                                        if (currentStyle.getSource(REGION_SOURCE) == null) {
                                            currentStyle.addSource(GeoJsonSource(REGION_SOURCE, json))
                                            currentStyle.addLayer(
                                                FillLayer(REGION_FILL, REGION_SOURCE).withProperties(
                                                    fillColor(Expression.rgb(235, 156, 50)),
                                                    fillOpacity(0.25f),
                                                    fillOutlineColor(Expression.rgb(120, 70, 10))
                                                )
                                            )
                                            currentStyle.addLayer(
                                                LineLayer(REGION_LINES, REGION_SOURCE).withProperties(
                                                    lineColor(Expression.rgb(120, 70, 10)),
                                                    lineWidth(1.1f)
                                                )
                                            )
                                            currentStyle.addLayer(
                                                FillLayer(REGION_SELECTED, REGION_SOURCE).withProperties(
                                                    fillColor(Expression.rgb(255, 193, 7)),
                                                    fillOpacity(0.72f),
                                                    fillOutlineColor(Expression.rgb(120, 70, 10))
                                                ).apply { setFilter(Expression.eq(Expression.get("id"), Expression.literal(-1))) }
                                            )
                                        } else {
                                            (currentStyle.getSource(REGION_SOURCE) as? GeoJsonSource)?.setGeoJson(json)
                                            (currentStyle.getLayer(REGION_SELECTED) as? FillLayer)?.setFilter(
                                                Expression.eq(Expression.get("id"), Expression.literal(-1))
                                            )
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                mainHandler.post { mapError = context.getString(R.string.map_load_regions_error) }
                            }
                        }
                        true
                    }
                }
            }
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 3.dp
        ) {
            Text(selectedName, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), style = MaterialTheme.typography.titleMedium)
        }
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { map?.animateCamera(CameraUpdateFactory.zoomIn()) }) { Text("+") }
            Button(onClick = { map?.animateCamera(CameraUpdateFactory.zoomOut()) }) { Text("−") }
            Button(onClick = {
                selectedIso2 = null
                selectedName = "برای انتخاب کشور روی نقشه بزنید"
                mapError = null
                map?.let { currentMap ->
                    currentMap.style?.let { style ->
                        (style.getLayer(COUNTRY_SELECTED) as? FillLayer)?.setFilter(
                            Expression.eq(Expression.get("iso2"), Expression.literal("__none__"))
                        )
                        clearRegionLayers(style)
                    }
                    currentMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder().target(LatLng(20.0, 0.0)).zoom(1.2).build()
                        ),
                        500
                    )
                }
            }) { Text(stringResource(R.string.map_reset)) }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = 2.dp
        ) {
            Text(stringResource(R.string.map_attribution), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
        }
        if (loading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        if (mapError != null) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)) {
                Text(mapError!!, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun clearRegionLayers(style: Style) {
    listOf(REGION_SELECTED, REGION_LINES, REGION_FILL).forEach { id ->
        if (style.getLayer(id) != null) style.removeLayer(id)
    }
    if (style.getSource(REGION_SOURCE) != null) style.removeSource(REGION_SOURCE)
}

private fun JsonElement?.safeJsonString(): String =
    if (this == null || isJsonNull) "" else runCatching { asString }.getOrDefault("")

private fun JsonElement?.safeJsonLong(): Long? =
    if (this == null || isJsonNull) null else runCatching { asLong }.getOrNull()
