plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Match the established RP Ideas release signer so verified updates install seamlessly.
val stableKeystore = rootProject.file("ci-keystore/release.p12")

android {
    sourceSets.getByName("main").java.srcDir(rootProject.file("shared/src/main/java"))
    namespace = "com.universalrp.smsguard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.universalrp.smsguard"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "1.1.11"
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
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
        textReport = true
        xmlReport = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

// Keep external-service availability separate from deterministic regression tests.
// The live assertion is unchanged and still fails its dedicated task when synthesis fails.
val liveSmsVoiceMethod = "com.universalrp.smsguard.SmsGuardEngineTest.liveOnlineNeuralStreamingSynthesizesRealMp3WhenNetworkAvailable"
val hdFailoverMethod = "com.universalrp.smsguard.SmsGuardEngineTest.liveHdVoiceFailoverSynthesizesWithinProductionDeadline"
tasks.withType<Test>().configureEach {
    if (name != "testLiveSmsVoice") filter { excludeTestsMatching(liveSmsVoiceMethod) }
    if (name != "testHdVoiceFailover") filter { excludeTestsMatching(hdFailoverMethod) }
}
tasks.register<Test>("testLiveSmsVoice") {
    group = "verification"
    description = "External neural voice availability probe; not a deterministic release test."
    dependsOn("testReleaseUnitTest")
    filter { includeTestsMatching(liveSmsVoiceMethod) }
    // Lazy providers are resolved before Gradle's NO-SOURCE check (doFirst is too late).
    testClassesDirs = files(provider { tasks.named<Test>("testReleaseUnitTest").get().testClassesDirs })
    classpath = files(provider { tasks.named<Test>("testReleaseUnitTest").get().classpath })
}

// Live application-level recovery is separate from the intentionally strict direct-voice probe.
tasks.register<Test>("testHdVoiceFailover") {
    group = "verification"
    description = "Real HD alternatives within the unchanged production deadline; no publication."
    dependsOn("testReleaseUnitTest")
    filter { includeTestsMatching(hdFailoverMethod) }
    testClassesDirs = files(provider { tasks.named<Test>("testReleaseUnitTest").get().testClassesDirs })
    classpath = files(provider { tasks.named<Test>("testReleaseUnitTest").get().classpath })
}
