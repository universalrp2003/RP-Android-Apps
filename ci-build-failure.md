# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/hardware/StorageVerification.kt:44:28 None of the following candidates is applicable:
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/hardware/StorageVerification.kt:44:52 Unresolved reference 'directory'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/hardware/StorageVerification.kt:46:37 Unresolved reference 'absolutePath'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/memory/RunningAppsManager.kt:130:26 Unresolved reference 'memoryInfo'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:29:36 Unresolved reference 'Focusable'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1745:13 Unresolved reference 'Focusable'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1746:17 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1747:28 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1787:94 Unresolved reference 'presetName'.
```

## Gradle summary
```
[Incubating] Problems report is available at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':tvguard:compileDebugKotlin'.
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

BUILD FAILED in 3m 1s
667 actionable tasks: 335 executed, 332 from cache
```
(no failing tests in 261 result file(s))

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
