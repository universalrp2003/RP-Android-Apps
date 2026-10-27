plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Match the established RP Ideas release signer so verified updates install seamlessly.
val stableKeystore = rootProject.file("ci-keystore/release.p12")

android {
    // shared/ is a plain source tree, not a Gradle module, so each app picks up the packages it
    // needs by path. This app takes the bilingual UI/update plumbing, which is what every other
    // module in this repository uses for its Tamil-first interface and in-app release notes.
    //
    // It deliberately does NOT take shared/.../voice or shared/.../weather. Vasanthi AI's output
    // is text, not speech: its user is deaf from birth, so there is nothing to hear and no reason
    // to drag a neural voice engine and a web socket into the APK. The speech recognition in this
    // app goes the other way - audio in, Tamil text out - through the platform SpeechRecognizer.
    sourceSets.getByName("main").java.srcDir(
        rootProject.file("shared/src/main/java/com/universalrp/updates"),
    )
    namespace = "com.universalrp.vasanthi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.universalrp.vasanthi"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.1.3"
        vectorDrawables.useSupportLibrary = true
        // ARM only, the same restriction Vocal Remover already carries. The phone this app is for
        // is a 4 GB Samsung, which is always ARM; the x86 and x86_64 libraries exist for emulators
        // and would be several megabytes of download for nothing.
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

    // The YAMNet model is memory-mapped straight out of the APK by the TFLite interpreter, so it
    // must be stored uncompressed. AGP adds .tflite to noCompress by default, but this is the one
    // line that keeps a silent "could not load model" off the owner's sister's phone, so it is
    // stated rather than assumed.
    androidResources { noCompress += "tflite" }

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

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // YAMNet, the audio-event classifier. Pinned to 2.16.1 rather than 2.17.0: 2.17.0 was
    // reported missing from Maven for a long stretch, and a dependency that fails to resolve
    // costs a whole CI round for no benefit. The Apache-2.0 model itself is committed under
    // src/main/assets with its own provenance recorded in the module README.
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    // English->Tamil translation, on device. The recogniser returns Tamil or English depending on
    // what the speaker used, and the owner asked for English speech to arrive as Tamil, so it has
    // to be translated rather than dropped.
    implementation("com.google.mlkit:translate:17.0.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.json:json:20240303")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
