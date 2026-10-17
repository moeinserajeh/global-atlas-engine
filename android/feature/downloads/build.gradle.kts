plugins { id("com.android.library"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.dagger.hilt.android"); id("com.google.devtools.ksp") }
android { namespace = "com.globalatlas.feature.downloads"; compileSdk = 37; defaultConfig { minSdk = 26 }; buildFeatures { compose = true } }
dependencies {
 implementation(project(":core:database")); implementation(project(":core:datastore")); implementation(project(":core:network"))
 implementation(platform("androidx.compose:compose-bom:2026.09.00")); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.material3:material3")
 implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0"); implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0"); implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
 implementation("com.google.dagger:hilt-android:2.60.1"); ksp("com.google.dagger:hilt-compiler:2.60.1")
 implementation("com.google.code.gson:gson:2.13.2")
 implementation("com.squareup.retrofit2:retrofit:3.0.0")
}
