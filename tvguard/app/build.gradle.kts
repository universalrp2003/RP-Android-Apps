plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val stableKeystore = rootProject.file("ci-keystore/release.p12")
val releaseStorePassword = System.getenv("RP_RELEASE_KEYSTORE_PASSWORD")
// Only CI's disposable-emulator upgrade fixture uses the older code. Never ship it.
val upgradeFixture = providers.gradleProperty("tvguardSignedUpgradeFixture").orNull == "true"
val requireStableSigner = providers.gradleProperty("tvguardRequireStableSigner").orNull == "true"
if (upgradeFixture && !requireStableSigner) {
    throw GradleException("The disposable upgrade fixture requires the stable signer")
}
if (requireStableSigner && (!stableKeystore.exists() || releaseStorePassword.isNullOrBlank())) {
    throw GradleException("The stable TV Guard signing identity is required for this candidate")
}

android {
    // Reuse the suite's single official links and sister-app directory.
    sourceSets.getByName("main").java.srcDir(
        rootProject.file("shared/src/main/java/com/universalrp/updates"),
    )
    namespace = "com.universalrp.tvguard"
    compileSdk = 36
    // Keep the release-test default; allow a disposable emulator to use a debug app.
    testBuildType = (project.findProperty("tvguardTestBuildType") as? String) ?: "release"

    defaultConfig {
        applicationId = "com.universalrp.tvguard"
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The signed 1.0 test candidate is code 2; later public updates must
        // increase this code and use the same signing identity. Code 3 added the status
        // overlay; code 4 fixes the long press on remotes that send no auto-repeat, makes the
        // overlay plain and movable, and removes the AI feature. It updates code 3 in place.
        versionCode = if (upgradeFixture) 1 else 4
        // The upgrade fixture deliberately impersonates the older 1.0 / code 1 install so a
        // same-signer in-place upgrade can be exercised; only the real candidate moves on.
        versionName = if (upgradeFixture) "1.0" else "1.2"
        vectorDrawables.useSupportLibrary = true
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
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
        debug {
            if (stableKeystore.exists() && !releaseStorePassword.isNullOrBlank()) signingConfig = signingConfigs.getByName("stable")
        }
        release {
            isMinifyEnabled = false
            if (stableKeystore.exists() && !releaseStorePassword.isNullOrBlank()) signingConfig = signingConfigs.getByName("stable")
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

    lint {
        textReport = true
        xmlReport = true
        disable += listOf("QueryAllPackagesPermission", "ScopedStorage")
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

    // D-pad/Home integration tests run only when connectedAndroidTest is requested.
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
