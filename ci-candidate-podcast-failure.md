# podcast candidate compile failure
Source: 8126ba993c7d757f0069838f0f840c6de930b75c
```
> Configure project :podcast
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/build.gradle.kts:54:21: 'jvmTarget: String' is deprecated. Please migrate to the compilerOptions DSL. More details are here: https://kotl.in/u1r8ln

> Configure project :vasanthi
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/build.gradle.kts:66:21: 'jvmTarget: String' is deprecated. Please migrate to the compilerOptions DSL. More details are here: https://kotl.in/u1r8ln

> Task :podcast:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :podcast:preBuild UP-TO-DATE
> Task :podcast:preDebugBuild UP-TO-DATE
> Task :podcast:checkDebugAarMetadata
> Task :podcast:processDebugNavigationResources FROM-CACHE
> Task :podcast:compileDebugNavigationResources FROM-CACHE
> Task :podcast:generateDebugResValues FROM-CACHE
> Task :podcast:mapDebugSourceSetPaths
> Task :podcast:generateDebugResources FROM-CACHE
> Task :podcast:mergeDebugResources FROM-CACHE
> Task :podcast:packageDebugResources FROM-CACHE
> Task :podcast:parseDebugLocalResources FROM-CACHE
> Task :podcast:createDebugCompatibleScreenManifests
> Task :podcast:extractDeepLinksDebug FROM-CACHE
> Task :podcast:processDebugMainManifest
> Task :podcast:processDebugManifest
> Task :podcast:javaPreCompileDebug FROM-CACHE
> Task :podcast:preDebugUnitTestBuild UP-TO-DATE
> Task :podcast:javaPreCompileDebugUnitTest FROM-CACHE
> Task :podcast:preReleaseBuild UP-TO-DATE
> Task :podcast:mergeReleaseJniLibFolders
> Task :podcast:mergeReleaseNativeLibs
> Task :podcast:mergeReleaseArtProfile

> Task :podcast:stripReleaseDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so. Run with --info option to learn more.

> Task :podcast:expandReleaseL8ArtProfileWildcards FROM-CACHE
> Task :podcast:extractReleaseNativeSymbolTables FROM-CACHE
> Task :podcast:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :podcast:buildKotlinToolingMetadata
> Task :podcast:processDebugManifestForPackage
> Task :podcast:checkReleaseDuplicateClasses
> Task :podcast:checkReleaseAarMetadata
> Task :podcast:processReleaseNavigationResources FROM-CACHE
> Task :podcast:compileReleaseNavigationResources FROM-CACHE
> Task :podcast:generateReleaseResValues FROM-CACHE
> Task :podcast:mapReleaseSourceSetPaths
> Task :podcast:generateReleaseResources FROM-CACHE
> Task :podcast:mergeReleaseResources FROM-CACHE
> Task :podcast:packageReleaseResources FROM-CACHE
> Task :podcast:parseReleaseLocalResources FROM-CACHE
> Task :podcast:createReleaseCompatibleScreenManifests
> Task :podcast:extractDeepLinksRelease FROM-CACHE
> Task :podcast:processReleaseMainManifest
> Task :podcast:processReleaseManifest
> Task :podcast:javaPreCompileRelease FROM-CACHE
> Task :podcast:processDebugResources
> Task :podcast:desugarReleaseFileDependencies FROM-CACHE
> Task :podcast:processReleaseManifestForPackage
> Task :podcast:mergeReleaseStartupProfile
> Task :podcast:mergeExtDexRelease FROM-CACHE
> Task :podcast:processReleaseResources
> Task :podcast:mergeReleaseShaders
> Task :podcast:compileReleaseShaders NO-SOURCE
> Task :podcast:generateReleaseAssets UP-TO-DATE
> Task :podcast:mergeReleaseAssets
> Task :podcast:compressReleaseAssets FROM-CACHE
> Task :podcast:extractReleaseVersionControlInfo
> Task :podcast:extractProguardFiles
> Task :podcast:collectReleaseDependencies
> Task :podcast:sdkReleaseDependencyData
> Task :podcast:optimizeReleaseResources
> Task :podcast:validateSigningRelease
> Task :podcast:writeReleaseAppMetadata
> Task :podcast:writeReleaseSigningConfigVersions

> Task :podcast:compileDebugKotlin
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt:296:44 'constructor(p0: Int, p1: CharSequence!, p2: PendingIntent?): Notification.Action.Builder' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt:298:44 'constructor(p0: Int, p1: CharSequence!, p2: PendingIntent?): Notification.Action.Builder' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/updates/AppUpdates.kt:354:27 'field ACTION_INSTALL_PACKAGE: String' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/updates/CallGuard.kt:44:89 'val callState: Int' is deprecated. Deprecated in Java.

> Task :podcast:compileDebugJavaWithJavac NO-SOURCE

> Task :podcast:compileReleaseKotlin
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt:296:44 'constructor(p0: Int, p1: CharSequence!, p2: PendingIntent?): Notification.Action.Builder' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt:298:44 'constructor(p0: Int, p1: CharSequence!, p2: PendingIntent?): Notification.Action.Builder' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/updates/AppUpdates.kt:354:27 'static static field ACTION_INSTALL_PACKAGE: String' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/updates/CallGuard.kt:44:89 'val callState: Int' is deprecated. Deprecated in Java.

> Task :podcast:bundleDebugClassesToRuntimeJar
> Task :podcast:bundleDebugClassesToCompileJar
> Task :podcast:processDebugJavaRes
> Task :podcast:compileReleaseJavaWithJavac NO-SOURCE
> Task :podcast:compileDebugUnitTestKotlin
> Task :podcast:dexBuilderRelease
> Task :podcast:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :podcast:processDebugUnitTestJavaRes

> Task :podcast:testDebugUnitTest FAILED

BuiltInShowsTest > catalogueIsCategorisedAndHasNoDuplicateIdsOrFeeds FAILED
    java.lang.AssertionError at BuiltInShowsTest.kt:9

59 tests completed, 1 failed
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/compile-1793099656687.json

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':podcast:testDebugUnitTest'.
> There were failing tests. See the report at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/build/reports/tests/testDebugUnitTest/index.html

* Try:
> Run with --scan to get full insights.

* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':podcast:testDebugUnitTest'.
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.lambda$executeIfValid$1(ExecuteActionsTaskExecuter.java:130)
	at org.gradle.internal.Try$Failure.ifSuccessfulOrElse(Try.java:293)
--
BUILD FAILED in 1m 1s
64 actionable tasks: 40 executed, 24 from cache
```
