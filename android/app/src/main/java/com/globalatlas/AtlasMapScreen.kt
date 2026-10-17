package com.globalatlas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.globalatlas.feature.atlas.AtlasMapScreen as FeatureAtlasMapScreen

@Composable
fun AtlasMapScreen(modifier: Modifier = Modifier, onCountrySelected: (iso2: String, name: String) -> Unit = { _, _ -> }) {
    FeatureAtlasMapScreen(modifier = modifier, onCountrySelected = onCountrySelected)
}
