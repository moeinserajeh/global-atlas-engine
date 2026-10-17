plugins { id("com.android.library"); id("com.google.dagger.hilt.android"); id("com.google.devtools.ksp") }
android { namespace = "com.globalatlas.core.network"; compileSdk = 37; defaultConfig { minSdk = 26 } }
dependencies {
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
}
