# podcast candidate compile failure
Source: 0dcebcd07fba481ffb18b39c2b0fe8751d580528
```
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
> Task :podcast:processDebugManifestForPackage
> Task :podcast:mergeReleaseArtProfile

> Task :podcast:stripReleaseDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so. Run with --info option to learn more.

> Task :podcast:processDebugResources
> Task :podcast:expandReleaseL8ArtProfileWildcards FROM-CACHE
> Task :podcast:extractReleaseNativeSymbolTables FROM-CACHE
> Task :podcast:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :podcast:checkReleaseDuplicateClasses
> Task :podcast:buildKotlinToolingMetadata
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
> Task :podcast:processReleaseManifestForPackage
> Task :podcast:processReleaseResources
> Task :podcast:desugarReleaseFileDependencies FROM-CACHE
> Task :podcast:mergeReleaseStartupProfile
> Task :podcast:mergeExtDexRelease FROM-CACHE
> Task :podcast:mergeReleaseShaders
> Task :podcast:compileReleaseShaders NO-SOURCE
> Task :podcast:generateReleaseAssets UP-TO-DATE
> Task :podcast:mergeReleaseAssets
> Task :podcast:compressReleaseAssets FROM-CACHE
> Task :podcast:extractReleaseVersionControlInfo
> Task :podcast:extractProguardFiles
> Task :podcast:collectReleaseDependencies
> Task :podcast:sdkReleaseDependencyData
> Task :podcast:validateSigningRelease
> Task :podcast:writeReleaseAppMetadata
> Task :podcast:writeReleaseSigningConfigVersions
> Task :podcast:optimizeReleaseResources

> Task :podcast:compileDebugKotlin
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt:280:44 'constructor(p0: Int, p1: CharSequence!, p2: PendingIntent?): Notification.Action.Builder' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt:282:44 'constructor(p0: Int, p1: CharSequence!, p2: PendingIntent?): Notification.Action.Builder' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/updates/AppUpdates.kt:354:27 'field ACTION_INSTALL_PACKAGE: String' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/updates/CallGuard.kt:44:89 'val callState: Int' is deprecated. Deprecated in Java.

> Task :podcast:compileReleaseKotlin
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt:280:44 'constructor(p0: Int, p1: CharSequence!, p2: PendingIntent?): Notification.Action.Builder' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/src/main/java/com/universalrp/podcast/PodcastPlaybackService.kt:282:44 'constructor(p0: Int, p1: CharSequence!, p2: PendingIntent?): Notification.Action.Builder' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/updates/AppUpdates.kt:354:27 'static static field ACTION_INSTALL_PACKAGE: String' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/updates/CallGuard.kt:44:89 'val callState: Int' is deprecated. Deprecated in Java.

> Task :podcast:compileDebugJavaWithJavac NO-SOURCE
> Task :podcast:bundleDebugClassesToCompileJar
> Task :podcast:bundleDebugClassesToRuntimeJar
> Task :podcast:processDebugJavaRes
> Task :podcast:compileReleaseJavaWithJavac NO-SOURCE
> Task :podcast:compileDebugUnitTestKotlin
> Task :podcast:dexBuilderRelease
> Task :podcast:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :podcast:processDebugUnitTestJavaRes

> Task :podcast:testDebugUnitTest FAILED

PlaybackPolicyTest > fiveBuiltInsHaveUniqueUrlsAndTamilEnglishNames FAILED
    java.lang.AssertionError at PlaybackPolicyTest.kt:31

50 tests completed, 1 failed
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/compile-1793092332451.json

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
BUILD FAILED in 41s
64 actionable tasks: 40 executed, 24 from cache
```
