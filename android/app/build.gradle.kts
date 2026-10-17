plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.dagger.hilt.android")
 id("com.google.devtools.ksp")
}
android {
 namespace = "com.globalatlas"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.globalatlas"
  minSdk = 26
  targetSdk = 36
  versionCode = 1
  versionName = "0.2.2"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 buildFeatures { compose = true; buildConfig = true }
 buildTypes {
  debug { manifestPlaceholders["usesCleartextTraffic"] = "true" }
  release { manifestPlaceholders["usesCleartextTraffic"] = "false"; isMinifyEnabled = false }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }
dependencies {
 implementation(project(":feature:atlas"))
 implementation(project(":feature:country"))
 implementation(project(":feature:region"))
 implementation(project(":feature:search"))
 implementation(project(":feature:downloads"))
 implementation(project(":core:designsystem"))
 implementation(project(":core:database"))
 implementation(project(":core:datastore"))
 implementation(platform("androidx.compose:compose-bom:2025.08.01"))
 implementation("androidx.activity:activity-compose:1.13.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.navigation:navigation-compose:2.9.6")
 implementation("com.google.dagger:hilt-android:2.60.1")
 ksp("com.google.dagger:hilt-compiler:2.60.1")
 testImplementation("junit:junit:4.13.2")
}
