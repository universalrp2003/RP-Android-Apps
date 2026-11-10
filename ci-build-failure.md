# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1161:46 Unresolved reference 'emmcSectors'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1162:39 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1162:39 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1162:43 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1165:50 Unresolved reference 'reportedPartitionsSumBytes'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1172:17 Inapplicable candidate(s): fun DetailRow(label: String, value: String): Unit
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1174:29 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1174:29 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1174:29 Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch:
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1174:33 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1174:105 Inapplicable candidate(s): fun <K> ELVIS_CALL(x: K?, y: K): K
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1181:85 Unresolved reference 'partitionsFound'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1192:89 Unresolved reference 'reportedAndroidVersion'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1193:79 Unresolved reference 'actualAndroidVersion'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1193:114 Unresolved reference 'actualCodeName'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1193:147 Unresolved reference 'underlyingSdkApiLevel'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1194:72 Unresolved reference 'underlyingSdkApiLevel'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1196:56 Unresolved reference 'buildTimeEpoch'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1600:65 Unresolved reference 'wifiSsid'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1601:17 Inapplicable candidate(s): fun DetailRow(label: String, value: String): Unit
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1601:55 Unresolved reference 'wifiFrequencyMhz'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1601:73 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1601:73 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1601:77 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1601:98 'operator' modifier is required on 'fun <T> Comparable<T>.compareTo(other: T): Int'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1601:144 Inapplicable candidate(s): fun <K> ELVIS_CALL(x: K?, y: K): K
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1602:17 Inapplicable candidate(s): fun DetailRow(label: String, value: String): Unit
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1602:62 Unresolved reference 'wifiRssiDbm'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1602:75 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1602:75 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1602:79 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1602:120 Inapplicable candidate(s): fun <K> ELVIS_CALL(x: K?, y: K): K
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1604:63 Unresolved reference 'localIpAddress'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tvguard/app/src/main/java/com/universalrp/tvguard/ui/MainActivity.kt:1976:72 Argument type mismatch: actual type is 'Alignment.Horizontal', but 'Alignment' was expected.
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

BUILD FAILED in 44s
977 actionable tasks: 459 executed, 518 from cache
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
