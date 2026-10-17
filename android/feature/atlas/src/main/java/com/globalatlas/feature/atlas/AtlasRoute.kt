package com.globalatlas.feature.atlas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier

@Composable
fun AtlasRoute(viewModel: AtlasViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { viewModel.dispatch(AtlasIntent.Load) }
    AtlasMapScreen(modifier = Modifier)
}
