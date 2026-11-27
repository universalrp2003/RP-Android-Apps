#!/usr/bin/env bash
# No package changes except installations on a disposable Android TV emulator.
set -euo pipefail
: "${RUNNER_TEMP:?missing CI scratch directory}"
work="$RUNNER_TEMP/disabler-signed"
pkg=com.universalrp.appdisabler
runner=com.universalrp.appdisabler.test/androidx.test.runner.AndroidJUnitRunner
old="$work/old-0.4.1.apk"
candidate="$work/System-App-Disabler-0.5.0-signed-candidate.apk"
test_apk="$work/disabler-release-androidTest.apk"
case "$(adb get-serialno | tr -d '\r')" in emulator-*) ;; *) echo 'Refusing a non-emulator' >&2; exit 1;; esac
for f in "$old" "$candidate" "$test_apk"; do test -s "$f"; done
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install "$old"
adb install -t "$test_apk"
uid_before="$(adb shell cmd package list packages -U "$pkg" | tr -d '\r' | sed -n "s/^package:$pkg uid://p")"
test -n "$uid_before"
probe() {
  local phase="$1"
  adb shell am instrument -w -r -e class com.universalrp.appdisabler.SignedUpdateTest \
    -e disablerUpgradePhase "$phase" "$runner" | tr -d '\r' | tee "$work/upgrade-$phase.log"
  python3 tvguard/tools/verify_instrumentation.py "$work/upgrade-$phase.log" \
    --class-name com.universalrp.appdisabler.SignedUpdateTest --test preserveSyntheticStateAcrossUpdate
}
probe seed
adb install -r "$candidate"
uid_after="$(adb shell cmd package list packages -U "$pkg" | tr -d '\r' | sed -n "s/^package:$pkg uid://p")"
test "$uid_before" = "$uid_after"
# Consume the complete dump before checking it: grep -q can exit on an early match and
# SIGPIPE adb/tr under pipefail even though the version is correct (exit status 141).
adb shell dumpsys package "$pkg" | tr -d '\r' > "$work/package-after-upgrade.txt"
grep -Fq 'versionName=0.5.0' "$work/package-after-upgrade.txt"
probe verify
adb shell am instrument -w -r -e class com.universalrp.appdisabler.TvFocusTest "$runner" \
  | tr -d '\r' | tee "$work/tv-focus.log"
python3 tvguard/tools/verify_instrumentation.py "$work/tv-focus.log" \
  --class-name com.universalrp.appdisabler.TvFocusTest \
  --test navigationAndPlanAreReachableByRemote \
  --test guideBackButtonAndSystemBackReturnToSetup \
  --test appCardOpensDetailsWithoutDisablingAnything \
  --test searchAndFilterControlsWorkWithRemote
probe verify
mapfile -t paths < <(adb shell pm path "$pkg" | tr -d '\r' | sed -n 's/^package://p')
test "${#paths[@]}" -eq 1
installed="$(adb shell sha256sum "${paths[0]}" | tr -d '\r' | awk '{print $1}')"
expected="$(sha256sum "$candidate" | awk '{print $1}')"
test "$installed" = "$expected"
echo "::notice title=Signed Disabler TV checks::Public 0.4.1 updated in place; UID and synthetic preferences, inventory and private file survived; four TV focus tests passed on SHA-256 $expected. No real package was disabled."
