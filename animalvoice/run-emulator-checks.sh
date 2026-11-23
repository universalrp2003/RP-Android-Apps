#!/usr/bin/env bash
set -euo pipefail
exec > >(tee "$RUNNER_TEMP/animal-setup.txt") 2>&1
set -x
[[ "$(adb get-serialno)" == emulator-* ]]
adb install "$(find animalvoice/app/build/outputs/apk/debug -maxdepth 1 -name '*.apk' -print -quit)"
adb install "$(find animalvoice/app/build/outputs/apk/androidTest/debug -maxdepth 1 -name '*.apk' -print -quit)"
# Large-text layout check: the identify controls and the Settings screen must stay reachable at 2x font scale.
adb shell settings put system font_scale 2.0
adb shell am instrument -w -r com.universalrp.animalvoice.experimental.test/androidx.test.runner.AndroidJUnitRunner | tee "$RUNNER_TEMP/animal-native.txt"
grep -F 'OK (15 tests)' "$RUNNER_TEMP/animal-native.txt"
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$RUNNER_TEMP/animal-native.txt"
