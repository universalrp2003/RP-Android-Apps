# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:9:8 Unresolved reference 'okhttp3'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:10:8 Unresolved reference 'okhttp3'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:11:8 Unresolved reference 'okhttp3'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:12:8 Unresolved reference 'okhttp3'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:13:8 Unresolved reference 'okhttp3'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:14:8 Unresolved reference 'okio'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:315:27 Unresolved reference 'OkHttpClient'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:315:40 Property delegate must have a 'getValue(SharedNeuralVoiceEngine, KProperty1<SharedNeuralVoiceEngine, ERROR CLASS: Symbol not found for OkHttpClient>)' method. None of the following functions is applicable:
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:315:43 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:315:43 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:316:9 Unresolved reference 'OkHttpClient'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:561:23 Unresolved reference 'Request'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:582:27 Unresolved reference 'newWebSocket'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:584:22 Unresolved reference 'WebSocketListener'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:585:17 'onOpen' overrides nothing.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:585:48 Unresolved reference 'WebSocket'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:585:69 Unresolved reference 'Response'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:606:31 Unresolved reference 'send'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:607:31 Unresolved reference 'send'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:610:17 'onMessage' overrides nothing.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:610:51 Unresolved reference 'WebSocket'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:613:25 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:613:49 Unresolved reference 'close'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:618:17 'onMessage' overrides nothing.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:618:51 Unresolved reference 'WebSocket'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:618:69 Unresolved reference 'ByteString'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:620:29 Unresolved reference 'size'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:621:39 Cannot infer type for type parameter 'V'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:621:75 Cannot infer type for type parameter 'V'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:623:43 Unresolved reference 'size'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:624:84 Unresolved reference 'size'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:625:70 Unresolved reference 'size'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:627:72 Unresolved reference 'size'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:633:17 'onFailure' overrides nothing.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:633:51 Unresolved reference 'WebSocket'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:633:86 Unresolved reference 'Response'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:635:55 Unresolved reference 'header'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:640:17 'onClosed' overrides nothing.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:640:50 Unresolved reference 'WebSocket'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt:648:13 Cannot infer type for type parameter 'R'. Specify it explicitly.
```

## Gradle summary
```
[Incubating] Problems report is available at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':expensetracker:compileDebugKotlin'.
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

BUILD FAILED in 2m 56s
224 actionable tasks: 99 executed, 125 from cache
```

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
