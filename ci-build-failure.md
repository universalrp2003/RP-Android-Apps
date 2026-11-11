# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/hardware/HardwareAuditor.kt:140:25 Unresolved reference 'StorageVerification'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/hardware/StorageVerification.kt:164:1 Syntax error: Unclosed comment.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/overlay/OverlaySampler.kt:104:53 Unresolved reference 'StorageTruth'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1982:21 Inapplicable candidate(s): fun DetailRow(label: String, value: String): Unit
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1984:58 Unresolved reference 'StorageTruth'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1984:97 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1984:97 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1984:101 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1987:40 Unresolved reference 'freeBytes'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1988:40 Unresolved reference 'totalBytes'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1989:40 Unresolved reference 'capped'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1991:27 Inapplicable candidate(s): fun <K> ELVIS_CALL(x: K?, y: K): K
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

BUILD FAILED in 5m 1s
691 actionable tasks: 354 executed, 337 from cache
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
