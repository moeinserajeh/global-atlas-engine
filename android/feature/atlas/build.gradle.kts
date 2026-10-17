plugins { id("com.android.library"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.dagger.hilt.android"); id("com.google.devtools.ksp") }
android { namespace = "com.globalatlas.feature.atlas"; compileSdk = 36; defaultConfig { minSdk = 26 }; buildFeatures { compose = true } }
dependencies {
 implementation(project(":core:common")); implementation(project(":core:model")); implementation(project(":core:designsystem")); implementation(project(":core:network")); implementation(project(":core:map")); implementation("org.maplibre.gl:android-sdk:11.8.0")
 implementation(platform("androidx.compose:compose-bom:2025.08.01")); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.ui:ui-tooling-preview"); implementation("androidx.compose.material3:material3")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4"); implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
 implementation("com.google.dagger:hilt-android:2.60.1"); ksp("com.google.dagger:hilt-compiler:2.60.1")
 implementation("com.squareup.retrofit2:retrofit:3.0.0")
 implementation("com.google.code.gson:gson:2.14.0")
}
