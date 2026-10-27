# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:4:24 Conflicting import: imported name 'Intent' is ambiguous.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:6:24 Conflicting import: imported name 'Intent' is ambiguous.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:128:54 Unresolved reference 'Context'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:129:29 Unresolved reference 'KEY_ASKED_HOME'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:136:50 Unresolved reference 'Context'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:138:25 Unresolved reference 'KEY_ASKED_HOME'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:454:5 Unresolved reference 'Card'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:457:18 Unresolved reference 'CardDefaults'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:459:9 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:510:13 Unresolved reference 'OutlinedButton'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/launcher/src/main/java/com/universalrp/launcher/MainActivity.kt:514:17 @Composable invocations can only happen from the context of a @Composable function
```

## Gradle summary
```
[Incubating] Problems report is available at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':launcher:compileDebugKotlin'.
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

BUILD FAILED in 37s
584 actionable tasks: 253 executed, 331 from cache
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
