plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.globalatlas.core.datastore"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
}
