plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Match the established RP Ideas release signer so verified updates can be installed.
val stableKeystore = rootProject.file("ci-keystore/release.p12")
val releaseStorePassword = System.getenv("RP_RELEASE_KEYSTORE_PASSWORD")

android {
    sourceSets.getByName("main").java.srcDir(rootProject.file("shared/src/main/java"))
    namespace = "com.oorarivippu.voiceannouncer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oorarivippu.voiceannouncer"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "1.1.9"
        vectorDrawables.useSupportLibrary = true
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        // Emit text + XML lint reports for CI diagnostics.
        textReport = true
        xmlReport = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
