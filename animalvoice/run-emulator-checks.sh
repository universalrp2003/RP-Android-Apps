#!/usr/bin/env bash
set -euo pipefail
[[ "$(adb get-serialno)" == emulator-* ]]
adb install "$(find animalvoice/app/build/outputs/apk/debug -maxdepth 1 -name '*.apk' -print -quit)"
adb install "$(find animalvoice/app/build/outputs/apk/androidTest/debug -maxdepth 1 -name '*.apk' -print -quit)"
adb shell am instrument -w -r com.universalrp.animalvoice.experimental.test/androidx.test.runner.AndroidJUnitRunner | tee "$RUNNER_TEMP/animal-native.txt"
grep -F 'OK (2 tests)' "$RUNNER_TEMP/animal-native.txt"
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$RUNNER_TEMP/animal-native.txt"
