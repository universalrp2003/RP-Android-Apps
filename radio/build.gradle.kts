// Ramesh Radio (Tamilnadu FM Radio) - online Tamil FM + world news + local audio + 10-band EQ.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Same stable CI keystore as the other modules, so every APK keeps one signature.
val stableKeystore = rootProject.file("ci-keystore/release.p12")
val releaseStorePassword = System.getenv("RP_RELEASE_KEYSTORE_PASSWORD")

android {
    sourceSets.getByName("main").java.srcDir(rootProject.file("shared/src/main/java"))
    namespace = "com.universalrp.tamilnadufm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.universalrp.tamilnadufm"
        minSdk = 26
        targetSdk = 36
        versionCode = 30
        versionName = "1.30"
    }

    signingConfigs {
        if (stableKeystore.exists() && !releaseStorePassword.isNullOrBlank()) {
            create("stable") {
                storeFile = stableKeystore
                storePassword = releaseStorePassword
                keyAlias = "cleansweep"
                keyPassword = releaseStorePassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (stableKeystore.exists() && !releaseStorePassword.isNullOrBlank()) signingConfig = signingConfigs.getByName("stable")
        }
        debug {
            if (stableKeystore.exists() && !releaseStorePassword.isNullOrBlank()) signingConfig = signingConfigs.getByName("stable")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("org.json:json:20240303")
    // Media3/ExoPlayer: streaming (MP3/AAC + HLS), background playback, notification,
    // lock-screen controls and audio focus.
    val media3Version = "1.11.0"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coroutines: the station store, directory search and the sleep timer run here.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
