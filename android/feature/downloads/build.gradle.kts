plugins { id("com.android.library") }
android { namespace = "com.globalatlas.feature.downloads"; compileSdk = 37; defaultConfig { minSdk = 26 } }
dependencies { implementation(project(":core:database")); implementation(project(":core:datastore")); implementation(project(":core:network")) }