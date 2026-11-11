# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:405:2 Unresolved reference 'The'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:405:10 Syntax error: Expecting an element.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:405:18 Unresolved reference 'hub'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:405:30 Unresolved reference 'available'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:405:39 Syntax error: Unexpected tokens (use ';' to separate expressions on the same line).
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:405:45 Syntax error: Expecting a condition in parentheses '(...)'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:405:46 Unresolved reference 'the'.
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

BUILD FAILED in 5m 7s
668 actionable tasks: 334 executed, 334 from cache
```
(no failing tests in 162 result file(s))

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
