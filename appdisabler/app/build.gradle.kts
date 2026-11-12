plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val stableKeystore = rootProject.file("ci-keystore/release.p12")
val releaseStorePassword = System.getenv("RP_RELEASE_KEYSTORE_PASSWORD")

android {
    // SisterApps.kt is now copied into this module; no shared srcDir to avoid voice/okhttp transitive errors
    // sourceSets.getByName("main").java.srcDir(rootProject.file("shared/src/main/java"))

    namespace = "com.universalrp.appdisabler"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.universalrp.appdisabler"
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 1
        versionName = "0.1.0"
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            if (stableKeystore.exists() && !releaseStorePassword.isNullOrBlank()) signingConfig = signingConfigs.getByName("stable")
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

    lint {
        textReport = true
        xmlReport = true
        disable += listOf("QueryAllPackagesPermission")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Shizuku — shell privilege for disable-only (no uninstall, no root)
    // API + provider for binder to shell daemon (installed separately)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
}
