### Oor Arivippu failure summary
```
> Task :oorarivippu:buildKotlinToolingMetadata
> Task :oorarivippu:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :oorarivippu:preBuild UP-TO-DATE
> Task :oorarivippu:preReleaseBuild UP-TO-DATE
> Task :oorarivippu:checkReleaseAarMetadata
> Task :oorarivippu:processReleaseNavigationResources FROM-CACHE
> Task :oorarivippu:compileReleaseNavigationResources FROM-CACHE
> Task :oorarivippu:generateReleaseResValues FROM-CACHE
> Task :oorarivippu:mapReleaseSourceSetPaths
> Task :oorarivippu:generateReleaseResources FROM-CACHE
> Task :oorarivippu:mergeReleaseResources FROM-CACHE
> Task :oorarivippu:packageReleaseResources FROM-CACHE
> Task :oorarivippu:parseReleaseLocalResources FROM-CACHE
> Task :oorarivippu:createReleaseCompatibleScreenManifests
> Task :oorarivippu:extractDeepLinksRelease FROM-CACHE
> Task :oorarivippu:processReleaseMainManifest
> Task :oorarivippu:processReleaseManifest
> Task :oorarivippu:javaPreCompileRelease FROM-CACHE
> Task :oorarivippu:preReleaseUnitTestBuild UP-TO-DATE
> Task :oorarivippu:javaPreCompileReleaseUnitTest FROM-CACHE
> Task :oorarivippu:extractProguardFiles
> Task :oorarivippu:mergeReleaseJniLibFolders
> Task :oorarivippu:mergeReleaseNativeLibs
> Task :oorarivippu:desugarReleaseFileDependencies FROM-CACHE
> Task :oorarivippu:mergeReleaseStartupProfile
> Task :oorarivippu:checkReleaseDuplicateClasses

> Task :oorarivippu:stripReleaseDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so. Run with --info option to learn more.

> Task :oorarivippu:extractReleaseNativeSymbolTables FROM-CACHE
> Task :oorarivippu:processReleaseManifestForPackage
> Task :oorarivippu:mergeReleaseArtProfile
> Task :oorarivippu:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :oorarivippu:mergeExtDexRelease FROM-CACHE
> Task :oorarivippu:mergeReleaseShaders
> Task :oorarivippu:compileReleaseShaders NO-SOURCE
> Task :oorarivippu:generateReleaseAssets UP-TO-DATE
> Task :oorarivippu:processReleaseResources
> Task :oorarivippu:mergeReleaseAssets
> Task :oorarivippu:compressReleaseAssets FROM-CACHE
> Task :oorarivippu:extractReleaseVersionControlInfo
> Task :oorarivippu:collectReleaseDependencies
> Task :oorarivippu:optimizeReleaseResources
> Task :oorarivippu:sdkReleaseDependencyData
> Task :oorarivippu:validateSigningRelease
> Task :oorarivippu:writeReleaseAppMetadata
> Task :oorarivippu:writeReleaseSigningConfigVersions

> Task :oorarivippu:compileReleaseKotlin
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/LocationAnnouncementService.kt:146:22 This declaration overrides a deprecated member but is not marked as deprecated itself. Add the '@Deprecated' annotation or suppress the diagnostic.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/LocationAnnouncementService.kt:283:26 'static field FLAG_HANDLES_MEDIA_BUTTONS: Int' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/LocationAnnouncementService.kt:284:30 'static field FLAG_HANDLES_TRANSPORT_CONTROLS: Int' is deprecated. Deprecated in Java.
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/LocationAnnouncementService.kt:317:61 'constructor(p0: String!, p1: String!): Locale' is deprecated. Deprecated in Java.
--
> Task :oorarivippu:compileReleaseJavaWithJavac NO-SOURCE
> Task :oorarivippu:processReleaseJavaRes
> Task :oorarivippu:bundleReleaseClassesToRuntimeJar
> Task :oorarivippu:bundleReleaseClassesToCompileJar
> Task :oorarivippu:generateReleaseLintReportModel
> Task :oorarivippu:generateReleaseUnitTestLintModel
> Task :oorarivippu:compileReleaseUnitTestKotlin
> Task :oorarivippu:dexBuilderRelease
> Task :oorarivippu:compileReleaseUnitTestJavaWithJavac NO-SOURCE
> Task :oorarivippu:processReleaseUnitTestJavaRes

> Task :oorarivippu:testReleaseUnitTest FAILED

OorWeatherAlertTest > smartWeatherRulesRespectQuietHoursForRainAndNeverSkipCyclone FAILED
    java.lang.AssertionError at OorWeatherAlertTest.kt:22

--
> Task :oorarivippu:lintAnalyzeReleaseUnitTest
> Task :oorarivippu:lintAnalyzeRelease
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_2-1792584246271.json

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':oorarivippu:testReleaseUnitTest'.
> There were failing tests. See the report at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/oorarivippu/build/reports/tests/testReleaseUnitTest/index.html

* Try:
> Run with --scan to get full insights.

* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':oorarivippu:testReleaseUnitTest'.
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.lambda$executeIfValid$1(ExecuteActionsTaskExecuter.java:130)
```
