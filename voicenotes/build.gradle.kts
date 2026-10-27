// Voice Notes (குரல் குறிப்பு) - dictate a note in Tamil, hear it back, keep it on the phone.
//
// B1 of the roadmap: a standalone recorder-and-read-back app built on the neural-voice pipeline
// that Live Guard 2.71 proved. Notes stay in private app storage; selected text is sent only when
// the owner explicitly requests HD neural playback. No account is required.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Same stable CI keystore as the other modules, so every APK keeps one signature.
val stableKeystore = rootProject.file("ci-keystore/release.p12")

android {
    // The shared neural-voice engine, curated voices and the bilingual UI strings live here.
    sourceSets.getByName("main").java.srcDir(rootProject.file("shared/src/main/java"))
    namespace = "com.universalrp.voicenotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.universalrp.voicenotes"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        if (stableKeystore.exists()) {
            create("stable") {
                storeFile = stableKeystore
                storePassword = "cleansweep-ci"
                keyAlias = "cleansweep"
                keyPassword = "cleansweep-ci"
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (stableKeystore.exists()) signingConfig = signingConfigs.getByName("stable")
        }
        debug {
            if (stableKeystore.exists()) signingConfig = signingConfigs.getByName("stable")
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
    testImplementation("org.json:json:20240303")

    // The neural voice engine speaks over a web socket, so okhttp is a runtime need, not a test one.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // org.json is part of the Android SDK; the artifact is here so the store's round-trip is
    // testable on the JVM.
    implementation("org.json:json:20240303")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
