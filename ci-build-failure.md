# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:22:9 Platform declaration clash: The following declarations have the same JVM signature (setListening(Z)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:30:9 Platform declaration clash: The following declarations have the same JVM signature (setLevel(F)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:38:9 Platform declaration clash: The following declarations have the same JVM signature (setMode(Lcom/universalrp/vasanthi/RecognitionMode;)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:53:9 Platform declaration clash: The following declarations have the same JVM signature (setOverlayVisible(Z)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:57:9 Platform declaration clash: The following declarations have the same JVM signature (setPreferOffline(Z)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:74:5 Platform declaration clash: The following declarations have the same JVM signature (setPreferOffline(Z)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:78:5 Platform declaration clash: The following declarations have the same JVM signature (setListening(Z)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:82:5 Platform declaration clash: The following declarations have the same JVM signature (setMode(Lcom/universalrp/vasanthi/RecognitionMode;)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:86:5 Platform declaration clash: The following declarations have the same JVM signature (setLevel(F)V):
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/vasanthi/src/main/java/com/universalrp/vasanthi/VasanthiState.kt:90:5 Platform declaration clash: The following declarations have the same JVM signature (setOverlayVisible(Z)V):
```

## Gradle summary
```
[Incubating] Problems report is available at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':vasanthi:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.3/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 3m 31s
288 actionable tasks: 151 executed, 137 from cache
```
(no failing tests recorded in the JUnit XML)

## Store artwork step
```
wrote cleaner/fastlane/metadata/android/en-US/images/icon.png 7005 bytes
wrote cleaner/fastlane/metadata/android/ta-IN/images/icon.png 7005 bytes
wrote radio/fastlane/metadata/android/en-US/images/icon.png 4595 bytes
wrote radio/fastlane/metadata/android/ta-IN/images/icon.png 4595 bytes
wrote equalizer/fastlane/metadata/android/en-US/images/icon.png 4334 bytes
fonts: title='DejaVu-Sans-Bold' body='DejaVu-Sans'
wrote cleaner/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote cleaner/fastlane/metadata/android/ta-IN/images/featureGraphic.png
wrote radio/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote radio/fastlane/metadata/android/ta-IN/images/featureGraphic.png
wrote equalizer/fastlane/metadata/android/en-US/images/featureGraphic.png

cleaner/fastlane/metadata/android/en-US/images/featureGraphic.png        1024x500
cleaner/fastlane/metadata/android/ta-IN/images/featureGraphic.png        1024x500
equalizer/fastlane/metadata/android/en-US/images/featureGraphic.png      1024x500
radio/fastlane/metadata/android/en-US/images/featureGraphic.png          1024x500
radio/fastlane/metadata/android/ta-IN/images/featureGraphic.png          1024x500
```
