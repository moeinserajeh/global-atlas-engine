plugins { id("com.android.library") }
android { namespace = "com.globalatlas.core.map"; compileSdk = 36; defaultConfig { minSdk = 26 } }
dependencies { implementation("org.maplibre.gl:android-sdk:11.8.0") }