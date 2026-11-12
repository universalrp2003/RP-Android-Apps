# radio candidate compile failure
Source: 8f52da565ce2b72fb730cbca05b644f6e58bbb5e
```
> Configure project :podcast
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/podcast/build.gradle.kts:54:21: 'jvmTarget: String' is deprecated. Please migrate to the compilerOptions DSL. More details are here: https://kotl.in/u1r8ln

> Configure project :vasanthi
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/build.gradle.kts:66:21: 'jvmTarget: String' is deprecated. Please migrate to the compilerOptions DSL. More details are here: https://kotl.in/u1r8ln

> Task :radio:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :radio:preBuild UP-TO-DATE
> Task :radio:preDebugBuild UP-TO-DATE
> Task :radio:checkDebugAarMetadata
> Task :radio:processDebugNavigationResources
> Task :radio:compileDebugNavigationResources FROM-CACHE
> Task :radio:generateDebugResValues FROM-CACHE
> Task :radio:mapDebugSourceSetPaths
> Task :radio:generateDebugResources
> Task :radio:packageDebugResources
> Task :radio:createDebugCompatibleScreenManifests
> Task :radio:extractDeepLinksDebug
> Task :radio:mergeDebugResources
> Task :radio:parseDebugLocalResources
> Task :radio:processDebugMainManifest
> Task :radio:processDebugManifest
> Task :radio:javaPreCompileDebug FROM-CACHE
> Task :radio:preDebugUnitTestBuild UP-TO-DATE
> Task :radio:javaPreCompileDebugUnitTest FROM-CACHE
> Task :radio:preReleaseBuild UP-TO-DATE
> Task :radio:mergeReleaseJniLibFolders
> Task :radio:mergeReleaseNativeLibs
> Task :radio:buildKotlinToolingMetadata
> Task :radio:processDebugManifestForPackage
> Task :radio:checkReleaseAarMetadata
> Task :radio:checkReleaseDuplicateClasses

> Task :radio:stripReleaseDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so. Run with --info option to learn more.

> Task :radio:processReleaseNavigationResources FROM-CACHE
> Task :radio:extractReleaseNativeSymbolTables
> Task :radio:compileReleaseNavigationResources FROM-CACHE
> Task :radio:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :radio:generateReleaseResValues FROM-CACHE
> Task :radio:mapReleaseSourceSetPaths
> Task :radio:generateReleaseResources FROM-CACHE
> Task :radio:processDebugResources
> Task :radio:mergeReleaseResources
> Task :radio:packageReleaseResources
> Task :radio:parseReleaseLocalResources FROM-CACHE
> Task :radio:createReleaseCompatibleScreenManifests
> Task :radio:extractDeepLinksRelease FROM-CACHE
> Task :radio:processReleaseMainManifest
> Task :radio:processReleaseManifest
> Task :radio:javaPreCompileRelease FROM-CACHE
> Task :radio:desugarReleaseFileDependencies
> Task :radio:mergeReleaseStartupProfile
> Task :radio:compileDebugKotlin
> Task :radio:processReleaseManifestForPackage
> Task :radio:mergeExtDexRelease

> Task :radio:compileDebugKotlin FAILED
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt:384:28 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt:436:17 Unresolved reference 'TextButton'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt:439:22 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt:442:17 Unresolved reference 'TextButton'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt:443:21 @Composable invocations can only happen from the context of a @Composable function
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/compile-1793119669175.json

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':radio:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':radio:compileDebugKotlin'.
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.lambda$executeIfValid$1(ExecuteActionsTaskExecuter.java:130)
	at org.gradle.internal.Try$Failure.ifSuccessfulOrElse(Try.java:293)
--
BUILD FAILED in 1m 38s
42 actionable tasks: 31 executed, 11 from cache
```
