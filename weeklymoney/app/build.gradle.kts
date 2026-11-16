plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val sampleBuild = providers.gradleProperty("weeklymoneySample").orNull == "true"
android {
    namespace = "com.universalrp.weeklymoney"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.universalrp.weeklymoney.preview"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.8-preview"
        manifestPlaceholders["weeklymoneyLabel"] = if (sampleBuild) "வார வசூல் · DEMO 0.8" else "வார வசூல் · TEST 0.8"
        buildConfigField("boolean", "IS_SAMPLE", sampleBuild.toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets.getByName("main").java.srcDir("../core/src/main/kotlin")
    sourceSets.getByName("test").java.srcDir("../core/src/test/kotlin")
    sourceSets.getByName("androidTest").assets.srcDir("../samples")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions.unitTests.all { it.systemProperty("weeklymoney.samples", rootProject.file("weeklymoney/samples").absolutePath) }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        val password = System.getenv("RP_RELEASE_KEYSTORE_PASSWORD")
        val key = rootProject.file("ci-keystore/release.p12")
        if (key.exists() && !password.isNullOrBlank()) create("ownerPreview") {
            storeFile = key; storePassword = password; keyAlias = "cleansweep"; keyPassword = password; storeType = "PKCS12"
        }
    }
    buildTypes {
        // Old ephemeral debug signer was not retained. Install beside it, never force uninstall.
        debug {
            applicationIdSuffix = if (sampleBuild) ".sample" else ".notebook"
            signingConfig = signingConfigs.findByName("ownerPreview") ?: signingConfigs.getByName("debug")
        }
        release { isMinifyEnabled = false }
    }
}
dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    // Use maintained compatible matchers instead of Espresso's legacy library 1.3.
    androidTestImplementation("org.hamcrest:hamcrest-library:2.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
