plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.dagger.hilt.android")
 id("com.google.devtools.ksp")
}
android {
 namespace = "com.globalatlas"; compileSdk = 37
 defaultConfig { applicationId = "com.globalatlas"; minSdk = 26; targetSdk = 37; versionCode = 1; versionName = "0.2.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }
dependencies {
 implementation(project(":feature:atlas")); implementation(project(":core:designsystem"))
 implementation(platform("androidx.compose:compose-bom:2026.09.00"))
 implementation("androidx.activity:activity-compose:1.13.0"); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.material3:material3")
 implementation("com.google.dagger:hilt-android:2.60.1"); ksp("com.google.dagger:hilt-compiler:2.60.1")
 testImplementation("junit:junit:4.13.2")
}