plugins { id("com.android.library") }
android { namespace = "com.globalatlas.core.testing"; compileSdk = 36; defaultConfig { minSdk = 26 } }
dependencies { implementation("junit:junit:4.13.2") }