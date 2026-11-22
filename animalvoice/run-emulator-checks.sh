#!/usr/bin/env bash
set -euo pipefail
exec > >(tee "$RUNNER_TEMP/animal-setup.txt") 2>&1
set -x
[[ "$(adb get-serialno)" == emulator-* ]]
adb install "$(find animalvoice/app/build/outputs/apk/debug -maxdepth 1 -name '*.apk' -print -quit)"
adb install "$(find animalvoice/app/build/outputs/apk/androidTest/debug -maxdepth 1 -name '*.apk' -print -quit)"
adb push "$RUNNER_TEMP/birdnet.tflite" /data/local/tmp/birdnet.tflite
adb shell chmod 644 /data/local/tmp/birdnet.tflite
adb shell run-as com.universalrp.animalvoice.experimental mkdir -p files
adb shell run-as com.universalrp.animalvoice.experimental cp /data/local/tmp/birdnet.tflite files/birdnet-lite-6k.tflite
adb shell rm /data/local/tmp/birdnet.tflite
adb shell am instrument -w -r com.universalrp.animalvoice.experimental.test/androidx.test.runner.AndroidJUnitRunner | tee "$RUNNER_TEMP/animal-native.txt"
grep -F 'OK (4 tests)' "$RUNNER_TEMP/animal-native.txt"
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$RUNNER_TEMP/animal-native.txt"
