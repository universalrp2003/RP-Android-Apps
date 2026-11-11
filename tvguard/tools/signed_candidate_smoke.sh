#!/usr/bin/env bash
# Synthetic tests only: this script may uninstall TV Guard on a disposable emulator.
set -euo pipefail

: "${RUNNER_TEMP:?RUNNER_TEMP must name the CI scratch directory}"
work="$RUNNER_TEMP/tvguard-signed-candidate"
old="$RUNNER_TEMP/tvguard-old-debug/tvguard-debug.apk"
fixture="$work/synthetic-older-same-key.apk"
candidate="$work/Android-TV-Guard-1.1-signed-candidate.apk"
test_apk="$work/tvguard-release-androidTest.apk"
package=com.universalrp.tvguard
runner=com.universalrp.tvguard.test/androidx.test.runner.AndroidJUnitRunner

serial="$(adb get-serialno | tr -d '\r')"
case "$serial" in
  emulator-*) ;;
  *) echo 'Refusing to change packages on anything except a disposable emulator.' >&2; exit 1 ;;
esac
for path in "$old" "$fixture" "$candidate" "$test_apk"; do test -s "$path"; done
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard

# Reproduce the reported issue without touching a real TV. A failed replacement
# must leave the old install and its synthetic data intact.
adb install "$old"
adb shell "run-as $package sh -c 'mkdir -p files && printf old-data > files/tvguard-conflict-check'"
if adb install -r "$candidate" >"$work/incompatible-install.log" 2>&1; then
  echo 'A differently signed APK unexpectedly replaced the old test installation.' >&2
  exit 1
fi
grep -q 'INSTALL_FAILED_UPDATE_INCOMPATIBLE' "$work/incompatible-install.log"
old_data="$(adb shell run-as "$package" cat files/tvguard-conflict-check | tr -d '\r')"
test "$old_data" = 'old-data'
# This uninstall is exclusively on the disposable emulator checked above.
adb uninstall "$package"

echo 'PASS: different signing keys cannot replace one another; old synthetic data remained intact.'
adb install "$fixture"
adb install -t "$test_apk"

run_probe() {
  local phase="$1" out="$work/update-$1.log"
  adb shell am instrument -w -r \
    -e class com.universalrp.tvguard.TvSignedUpdateTest \
    -e tvguardUpgradePhase "$phase" "$runner" | tr -d '\r' | tee "$out"
  python3 tvguard/tools/verify_instrumentation.py "$out" \
    --class-name com.universalrp.tvguard.TvSignedUpdateTest --test exerciseUpdateState
}

run_probe seed
# Upgrade in place: the fixture and exact candidate have the same package and signer.
adb install -r "$candidate"
run_probe verify

adb shell am instrument -w -r \
  -e class com.universalrp.tvguard.TvRemoteFocusTest "$runner" |
  tr -d '\r' | tee "$work/tv-focus.log"
python3 tvguard/tools/verify_instrumentation.py "$work/tv-focus.log" \
  --class-name com.universalrp.tvguard.TvRemoteFocusTest \
  --test appOptionsDirectionsStayInDialogAndHomeResetsEvenWhileAlreadyHome \
  --test expertSwitchesNavigateWithDpadAndRevokeAndRestoreHub \
  --test downFromDebloaterHeaderEntersItsControlsAndHomeReturnsToFirstApp \
  --test networkAndAboutHeadersLeadIntoTheirOwnContent \
  --test appendedExpertSwitchesAreReachableScrolledAndOffByDefault
# The long-press regression: real key-down / hold / key-up, then D-pad into the menu.
adb shell am instrument -w -r \
  -e class com.universalrp.tvguard.TvLongPressOptionsTest "$runner" |
  tr -d '\r' | tee "$work/tv-longpress.log"
python3 tvguard/tools/verify_instrumentation.py "$work/tv-longpress.log" \
  --class-name com.universalrp.tvguard.TvLongPressOptionsTest \
  --test longPressOpensOptionsAndReleaseDoesNotLaunchOrClose \
  --test shortPressStillLaunchesOrKeepsFocusOnTheLauncher \
  --test aRemoteThatAutoRepeatsAlsoOpensTheMenuExactlyOnce \
  --test dismissingOptionsRestoresFocusToTheTile \
  --test afterTheMenuClosesAShortPressLaunchesAgain

# Also check that the focus tests restored the synthetic choices.
run_probe verify

# Verify the APK exercised by the emulator is exactly the candidate to be offered.
mapfile -t installed_paths < <(adb shell pm path "$package" | tr -d '\r' | sed -n 's/^package://p')
test "${#installed_paths[@]}" -eq 1
installed_sha="$(adb shell sha256sum "${installed_paths[0]}" | tr -d '\r' | awk '{print $1}')"
candidate_sha="$(sha256sum "$candidate" | awk '{print $1}')"
test "$installed_sha" = "$candidate_sha"
echo "::notice title=Signed TV Guard emulator checks::Different-signature replacement rejected; same-key code 1 to code 3 update preserved synthetic favorites, hidden apps, feature choices and a private file; four TV focus tests and four real key-down/hold/up long-press tests passed on the exact candidate APK."
