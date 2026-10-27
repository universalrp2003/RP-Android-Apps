plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val stableKeystore = rootProject.file("ci-keystore/release.p12")

android {
    sourceSets.getByName("main").java.srcDir(
        rootProject.file("shared/src/main/java/com/universalrp/updates"),
    )
    namespace = "com.universalrp.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.universalrp.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"
        vectorDrawables.useSupportLibrary = true
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
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
        debug {
            if (stableKeystore.exists()) signingConfig = signingConfigs.getByName("stable")
        }
        release {
            isMinifyEnabled = false
            if (stableKeystore.exists()) signingConfig = signingConfigs.getByName("stable")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    lint { textReport = true; xmlReport = true }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
