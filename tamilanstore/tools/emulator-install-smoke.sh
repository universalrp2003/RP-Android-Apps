#!/usr/bin/env bash
# Destructive fixture transitions ONLY on a fresh GitHub Actions emulator, never a phone.
set -euo pipefail
[[ "${CI:-}" == true && "${GITHUB_ACTIONS:-}" == true ]]
[[ "$(adb get-serialno)" == emulator-* ]]
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]]
fixtures="$RUNNER_TEMP/store-fixtures"
evidence="$RUNNER_TEMP/store-emulator-evidence"
mkdir -p "$evidence"
adb root
adb wait-for-device
adb install -r "$(find tamilanstore/build/outputs/apk/debug -maxdepth 1 -name '*.apk' | head -1)"
adb install -r "$(find tamilanstore/build/outputs/apk/androidTest/debug -maxdepth 1 -name '*.apk' | head -1)"
adb shell appops set com.universalrp.tamilanstore REQUEST_INSTALL_PACKAGES allow
run_phase() {
    adb shell am instrument -w -r -e phase "$1" \
      -e class com.universalrp.tamilanstore.ArchiveVerificationTest \
      com.universalrp.tamilanstore.test/androidx.test.runner.AndroidJUnitRunner \
      | tee "$evidence/$1.txt"
    grep -q '^OK (' "$evidence/$1.txt"
    ! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$evidence/$1.txt"
}
# Fresh APK -> real package parser, pinned cert, FileProvider and installer confirmation.
run_phase fresh
# A legacy-key install is preserved and blocked rather than offered an unsafe update.
adb install "$fixtures/DailyVault-v0.3.apk"
adb shell 'mkdir -p /data/user/0/com.universalrp.dailyvault/files; echo synthetic-ci-only > /data/user/0/com.universalrp.dailyvault/files/store-preservation-marker'
before="$(adb shell sha256sum /data/user/0/com.universalrp.dailyvault/files/store-preservation-marker)"
run_phase legacy
[[ "$before" == "$(adb shell sha256sum /data/user/0/com.universalrp.dailyvault/files/store-preservation-marker)" ]]
# Remove ONLY this synthetic emulator fixture to test a different signing lineage.
adb uninstall com.universalrp.dailyvault
adb install "$fixtures/DailyVault-v0.4.apk"
run_phase current
adb pull /sdcard/Android/data/com.universalrp.tamilanstore/files/. "$evidence/"
echo 'PASS: fresh install confirmation, same-key update confirmation, legacy-key block, size/hash/identity rejection; no confirmed installation is claimed.'
