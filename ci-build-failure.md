# CI build failure excerpt

## Kotlin / resource errors
```
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:16:33 Unresolved reference 'ManagedActivityResultLauncher'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:121:60 Argument type mismatch: actual type is 'Float', but 'Int' was expected.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:175:53 Unresolved reference 'ManagedActivityResultLauncher'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:203:64 Argument type mismatch: actual type is 'String', but 'CoroutineContext' was expected.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:203:64 No value passed for parameter 'block'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:291:21 Unresolved reference 'ManagedActivityResultLauncher'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:292:24 Unresolved reference 'ManagedActivityResultLauncher'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:293:24 Unresolved reference 'ManagedActivityResultLauncher'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:326:60 Argument type mismatch: actual type is 'String', but 'CoroutineContext' was expected.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:326:60 No value passed for parameter 'block'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:362:102 Argument type mismatch: actual type is 'Array<String>', but 'CoroutineContext' was expected.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:362:102 No value passed for parameter 'block'.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:363:102 Argument type mismatch: actual type is 'Array<String>', but 'CoroutineContext' was expected.
e: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/animalvoice/app/src/main/java/com/universalrp/animalvoice/MainActivity.kt:363:102 No value passed for parameter 'block'.
```

## Gradle summary
```
[Incubating] Problems report is available at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':animalvoice:compileDebugKotlin'.
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

BUILD FAILED in 2m 46s
396 actionable tasks: 179 executed, 217 from cache
```
(no failing tests in 164 result file(s))

## Store artwork step
```
Setting up fonts-noto-mono (20201225-2build1) ...
Setting up libwmflite-0.2-7:amd64 (0.2.14-1) ...
Setting up libijs-0.35:amd64 (0.35-16) ...
Setting up libgs-common (10.06.0~dfsg-3ubuntu1.2) ...
Setting up libnetpbm11t64:amd64 (2:11.10.02-1build1) ...
Setting up poppler-data (0.4.12-1build1) ...
Setting up libjxr0t64:amd64 (1.2~git20170615.f752187-5.3build1) ...
Setting up libjbig2dec0:amd64 (0.20-1ubuntu0.26.04.1) ...
Setting up xfonts-encodings (1:1.0.5-0ubuntu3) ...
Setting up libopenexr-3-1-30:amd64 (3.1.13-2build1) ...
Setting up libidn12:amd64 (1.43-2ubuntu0.26.04.1) ...
Setting up netpbm (2:11.10.02-1build1) ...
Setting up imagemagick (8:7.1.2.18+dfsg1-1) ...
Setting up fonts-droid-fallback (1:8.1.0r7-1~1.gbp36536bbuild1) ...
Setting up libpaper2:amd64 (2.2.5-0.3maysync1) ...
Setting up libdjvulibre-text (3.5.29-1) ...
Setting up libjxr-tools (1.2~git20170615.f752187-5.3build1) ...
Setting up libdjvulibre21:amd64 (3.5.29-1) ...
Setting up libpaper-utils (2.2.5-0.3maysync1) ...
Setting up xfonts-utils (1:7.7+7build1) ...
Setting up libmagickcore-7.q16-10-extra:amd64 (8:7.1.2.18+dfsg1-1) ...
Setting up fonts-urw-base35 (20200910-8build1) ...
Setting up libgs10-common (10.06.0~dfsg-3ubuntu1.2) ...
Setting up libgs10:amd64 (10.06.0~dfsg-3ubuntu1.2) ...
Setting up ghostscript (10.06.0~dfsg-3ubuntu1.2) ...
Processing triggers for fontconfig (2.17.1-3ubuntu1) ...
Processing triggers for hicolor-icon-theme (0.18-2build1) ...
Processing triggers for libc-bin (2.43-2ubuntu2.7) ...
Processing triggers for man-db (2.13.1-1build1) ...
Not building database; man-db/auto-update is not 'true'.

Running kernel seems to be up-to-date.

Restarting services...

Service restarts being deferred:
 systemctl restart hosted-compute-agent.service

No containers need to be restarted.

No user sessions are running outdated binaries.

No VM guests are running outdated hypervisor (qemu) binaries on this host.
ImageMagick installed
fonts: title='DejaVu-Sans-Bold' body='DejaVu-Sans'
wrote cleaner/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote cleaner/fastlane/metadata/android/ta-IN/images/featureGraphic.png
wrote radio/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote radio/fastlane/metadata/android/ta-IN/images/featureGraphic.png
wrote equalizer/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote tvguard/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote tvguard/fastlane/metadata/android/ta-IN/images/featureGraphic.png

cleaner/fastlane/metadata/android/en-US/images/featureGraphic.png        1024x500
cleaner/fastlane/metadata/android/ta-IN/images/featureGraphic.png        1024x500
equalizer/fastlane/metadata/android/en-US/images/featureGraphic.png      1024x500
radio/fastlane/metadata/android/en-US/images/featureGraphic.png          1024x500
radio/fastlane/metadata/android/ta-IN/images/featureGraphic.png          1024x500
tvguard/fastlane/metadata/android/en-US/images/featureGraphic.png        1024x500
tvguard/fastlane/metadata/android/ta-IN/images/featureGraphic.png        1024x500
```
