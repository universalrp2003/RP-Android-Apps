# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:877:18 Syntax error: Expecting ')'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:878:14 Syntax error: Unexpected tokens (use ';' to separate expressions on the same line).
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:879:9 Syntax error: Expecting an element.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:5 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:8 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:9 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:24 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:26 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:26 Functions which invoke @Composable functions must be marked with the @Composable annotation
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:26 Function declaration must have a name.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:24 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:24 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:35 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:35 Inapplicable candidate(s): fun <T> mutableStateOf(value: T, policy: SnapshotMutationPolicy<T> = ...): MutableState<T>
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:50 Unresolved reference 'pack'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:69 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:69 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:73 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:137 Inapplicable candidate(s): fun <K> ELVIS_CALL(x: K?, y: K): K
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:884:9 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:885:34 Unresolved reference 'showQuotaDialog'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:897:53 Unresolved reference 'it'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:898:35 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:915:25 Unresolved reference 'vm'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:916:25 Unresolved reference 'showQuotaDialog'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:924:40 Unresolved reference 'showQuotaDialog'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:930:1 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:877:18 Syntax error: Expecting ')'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:878:14 Syntax error: Unexpected tokens (use ';' to separate expressions on the same line).
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:879:9 Syntax error: Expecting an element.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:5 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:8 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:9 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:24 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:26 Syntax error: Expecting a top level declaration.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:26 Functions which invoke @Composable functions must be marked with the @Composable annotation
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:882:26 Function declaration must have a name.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:24 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:24 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:35 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:35 Inapplicable candidate(s): fun <T> mutableStateOf(value: T, policy: SnapshotMutationPolicy<T> = ...): MutableState<T>
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:50 Unresolved reference 'pack'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:69 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:69 Cannot infer type for type parameter 'R'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:73 Cannot infer type for type parameter 'T'. Specify it explicitly.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:883:137 Inapplicable candidate(s): fun <K> ELVIS_CALL(x: K?, y: K): K
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:884:9 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:885:34 Unresolved reference 'showQuotaDialog'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:897:53 Unresolved reference 'it'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:898:35 @Composable invocations can only happen from the context of a @Composable function
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:915:25 Unresolved reference 'vm'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:916:25 Unresolved reference 'showQuotaDialog'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:924:40 Unresolved reference 'showQuotaDialog'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt:930:1 Syntax error: Expecting a top level declaration.
```

## Gradle summary
```
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
