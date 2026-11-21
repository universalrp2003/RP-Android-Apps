#!/bin/bash
set -euxo pipefail
trap 'echo "::error title=Launcher smoke::Failed at line $LINENO"' ERR
case "$(adb get-serialno)" in emulator-*) ;; *) echo 'Only disposable emulators allowed' >&2; exit 1;; esac
D="$RUNNER_TEMP/launcher18"; P=com.universalrp.launcher
[[ "${CI:-}" == true && "${GITHUB_ACTIONS:-}" == true ]]
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]]
adb shell settings put system screen_off_timeout 1800000
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install "$D/Elderly-Launcher-v1.7.apk"
uid=$(adb shell cmd package list packages -U "$P" | tr -d '\r' | awk -v p="package:$P" '$1 == p {sub(/^uid:/, "", $2); print $2}')
[[ "$uid" =~ ^[0-9]+$ ]]
adb install -r launcher/build/outputs/apk/androidTest/release/launcher-release-androidTest.apk
adb shell am instrument -w -r -e class com.universalrp.launcher.SeedUpgradeFixtureTest "$P.test/androidx.test.runner.AndroidJUnitRunner" | tee "$D/seed.txt"
grep -F 'OK (1 test)' "$D/seed.txt"
# This role assignment is strictly for the disposable emulator, not a user device.
adb shell cmd role add-role-holder --user 0 android.app.role.HOME "$P"
adb install -r "$D/Elderly-Launcher-v1.8.apk"
test "$uid" = "$(adb shell cmd package list packages -U "$P" | tr -d '\r' | awk -v p="package:$P" '$1 == p {sub(/^uid:/, "", $2); print $2}')"
installed=$(adb shell pm path "$P" | tr -d '\r' | sed -n 's/^package://p')
test "$(adb shell sha256sum "$installed" | awk '{print $1}')" = "$(sha256sum "$D/Elderly-Launcher-v1.8.apk" | awk '{print $1}')"
adb shell dumpsys role | grep -F "$P"
adb shell am instrument -w -r -e class com.universalrp.launcher.HomeReturnAndContactsTest#homeButtonAndSyntheticContacts "$P.test/androidx.test.runner.AndroidJUnitRunner" | tee "$D/native.txt"
grep -F 'OK (1 test)' "$D/native.txt"
grep -F 'INSTRUMENTATION_CODE: -1' "$D/native.txt"

# Exercise large fonts and a named remove action without making any phone calls.
adb shell settings put system font_scale 2.0
adb shell am force-stop "$P"
adb shell am instrument -w -r -e class com.universalrp.launcher.LargeContactLayoutTest#largeFontAndRemove "$P.test/androidx.test.runner.AndroidJUnitRunner" | tee "$D/large-font.txt"
grep -F 'OK (1 test)' "$D/large-font.txt"
adb shell am force-stop "$P"
adb shell am instrument -w -r -e class com.universalrp.launcher.LargeContactLayoutTest#restartRetainsOnlyTheOtherContacts "$P.test/androidx.test.runner.AndroidJUnitRunner" | tee "$D/restart.txt"
grep -F 'OK (1 test)' "$D/restart.txt"
adb shell settings put system font_scale 1.0
