#!/usr/bin/env bash
set -euo pipefail
exec > >(tee "$RUNNER_TEMP/v3-setup.log") 2>&1
[[ "$(adb get-serialno)" == emulator-* ]]
adb install "$(find animalvoice/app/build/outputs/apk/debug -maxdepth 1 -name '*.apk' -print -quit)"
adb install "$(find animalvoice/app/build/outputs/apk/androidTest/debug -maxdepth 1 -name '*.apk' -print -quit)"
adb push "$RUNNER_TEMP/v3.tflite" /data/local/tmp/v3.tflite
adb shell chmod 644 /data/local/tmp/v3.tflite
adb shell run-as com.universalrp.animalvoice.experimental mkdir -p files
adb shell run-as com.universalrp.animalvoice.experimental cp /data/local/tmp/v3.tflite files/research-birdnet3.tflite
adb shell rm /data/local/tmp/v3.tflite
adb shell am instrument -w -r -e class com.universalrp.animalvoice.BirdNetV3ResearchTest com.universalrp.animalvoice.experimental.test/androidx.test.runner.AndroidJUnitRunner | tee "$RUNNER_TEMP/v3-native.log"
grep -F 'OK (1 test)' "$RUNNER_TEMP/v3-native.log"
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$RUNNER_TEMP/v3-native.log"
