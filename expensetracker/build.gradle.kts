plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Match the established RP Ideas release signer so verified updates install seamlessly.
val stableKeystore = rootProject.file("ci-keystore/release.p12")

android {
    // shared/ is a plain source tree, not a Gradle module, so each app picks up the packages it
    // needs by path. This app declares no INTERNET permission, so it takes only the two packages
    // that make no network call: the anti-spoof finance parser, and the bilingual UI/update
    // plumbing. The voice and weather engines in shared/ speak over a web socket and would drag
    // okhttp into an app that cannot open a socket - dead weight in the APK, and a dependency that
    // contradicts the app's whole promise. Nothing is deleted: the seven apps that do hold the
    // permission still compile all of shared/.
    sourceSets.getByName("main").java.srcDir(
        rootProject.file("shared/src/main/java/com/universalrp/smsguard/engine"),
    )
    sourceSets.getByName("main").java.srcDir(
        rootProject.file("shared/src/main/java/com/universalrp/updates"),
    )
    namespace = "com.universalrp.expensetracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.universalrp.expensetracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        vectorDrawables.useSupportLibrary = true
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        textReport = true
        xmlReport = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // org.json ships with Android, so the APK needs nothing extra; the artifact is here purely so
    // the ledger's encode/decode round-trip is testable on the JVM. Without it the Android stubs
    // throw "not mocked" and the store's tests fail for a reason that has nothing to do with the
    // store. Same line every other module in this repository carries.
    testImplementation("org.json:json:20240303")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
