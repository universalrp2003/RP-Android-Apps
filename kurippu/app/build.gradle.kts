plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.universalrp.kurippu"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.universalrp.kurippu"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        val password = System.getenv("RP_RELEASE_KEYSTORE_PASSWORD")
        val key = rootProject.file("ci-keystore/release.p12")
        if (key.exists() && !password.isNullOrBlank()) create("ownerCandidate") {
            storeFile = key; storePassword = password; keyAlias = "cleansweep"; keyPassword = password; storeType = "PKCS12"
        }
    }
    buildTypes {
        debug { signingConfig = signingConfigs.findByName("ownerCandidate") ?: signingConfigs.getByName("debug") }
        release { signingConfig = signingConfigs.findByName("ownerCandidate"); isMinifyEnabled = false; isDebuggable = false }
    }
}
dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
