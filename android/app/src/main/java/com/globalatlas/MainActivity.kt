package com.globalatlas
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.globalatlas.core.designsystem.AtlasTheme
import com.globalatlas.feature.atlas.AtlasRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { AtlasTheme { AtlasRoute() } }
 }
}