#!/usr/bin/env bash
# Only a disposable CI emulator. No real contacts, calls, unknown-source grants or role changes.
set -euo pipefail
[[ "$CI" == true && "$GITHUB_ACTIONS" == true ]]
[[ "$(adb get-serialno)" == emulator-* ]]
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]]
root="$RUNNER_TEMP/launcher-home"
pkg=com.universalrp.launcher
mkdir -p "$root"
exec > >(tee "$root/smoke.log") 2>&1
trap 'rc=$?; echo "ERROR: smoke line $LINENO (status $rc)" >&2' ERR
package_uid() {
    adb shell cmd package list packages -U --user 0 "$pkg" | tr -d '\r' |
        awk -v target="package:$pkg" '$1 == target && $2 ~ /^uid:[0-9]+$/ {sub(/^uid:/, "", $2); print $2}'
}
adb root
adb wait-for-device
# A slow install must not leave the disposable emulator asleep behind its keyguard.
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
# Preserve the actual failing status, even while collecting bounded synthetic UI evidence.
trap 'status=$?; trap - EXIT; adb pull /sdcard/Android/data/com.universalrp.launcher/files/. "$root/ui/" || true; exit "$status"' EXIT
adb install "$root/Elderly-Launcher-v1.6.apk"
before="$(package_uid)"
[[ "$before" =~ ^[0-9]+$ ]]
adb shell "mkdir -p /data/user/0/$pkg/files /data/user/0/$pkg/shared_prefs"
adb shell "echo synthetic-launcher-upgrade > /data/user/0/$pkg/files/upgrade-fixture.txt"
adb shell "echo '<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?><map><boolean name=\"asked_for_home\" value=\"true\" /></map>' > /data/user/0/$pkg/shared_prefs/launcher.xml"
owner="$(adb shell stat -c %u "/data/user/0/$pkg" | tr -d '\r')"
[[ "$owner" =~ ^[0-9]+$ ]]
adb shell "echo '<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?><map><string name=\"list\"></string><string name=\"fixture\">synthetic-contact-pref</string></map>' > /data/user/0/$pkg/shared_prefs/launcher_favourites.xml"
adb shell "chown -R $owner:$owner /data/user/0/$pkg/files /data/user/0/$pkg/shared_prefs"
# Existing1.6 saved app shortcut, preserved byte-for-byte by the update.
adb shell "echo '<?xml version=\"1.0\" encoding=\"utf-8\" ?><map><string name=\"packages_v1\">[&quot;com.android.settings&quot;]</string></map>' > /data/user/0/$pkg/shared_prefs/launcher_favourite_apps.xml"
adb shell "chown $owner:$owner /data/user/0/$pkg/shared_prefs/launcher_favourite_apps.xml"
apps_before="$(adb shell sha256sum /data/user/0/$pkg/shared_prefs/launcher_favourite_apps.xml)"
contact_before="$(adb shell sha256sum /data/user/0/$pkg/shared_prefs/launcher_favourites.xml)"
adb install -r "$root/Elderly-Launcher-v1.7.apk"
[[ "$contact_before" == "$(adb shell sha256sum /data/user/0/$pkg/shared_prefs/launcher_favourites.xml)" ]]
[[ "$apps_before" == "$(adb shell sha256sum /data/user/0/$pkg/shared_prefs/launcher_favourite_apps.xml)" ]]
after="$(package_uid)"
[[ -n "$before" && "$before" == "$after" ]]
adb install -r "$(find launcher/build/outputs/apk/androidTest/release -name '*.apk' | head -1)"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell am instrument -w -r -e class com.universalrp.launcher.HomeFollowupTest \
  com.universalrp.launcher.test/androidx.test.runner.AndroidJUnitRunner | tee "$root/native.txt"
grep -q '^OK (' "$root/native.txt"
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$root/native.txt"
adb shell am force-stop "$pkg"
adb shell am instrument -w -r -e phase restart -e class com.universalrp.launcher.HomeFollowupTest \
  com.universalrp.launcher.test/androidx.test.runner.AndroidJUnitRunner | tee "$root/restart.txt"
grep -q '^OK (' "$root/restart.txt"
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=' "$root/restart.txt"
(cd "$root" && sha256sum -c Elderly-Launcher-v1.7.sha256)
printf 'PASS: same UID/private fixture/home preference; AM/PM; adjacent volume and actual levels; native app picker/add/open/persistence/remove; contact isolation; 200%% font reachability. No phone/Google/hardware audio proof.\n' | tee "$root/result.txt"
python3 - <<'PYRECEIPT'
import hashlib,json,os,pathlib
root=pathlib.Path(os.environ['RUNNER_TEMP'])/'launcher-home'
apk=root/'Elderly-Launcher-v1.7.apk'
receipt=dict(source=os.environ['GITHUB_SHA'],package='com.universalrp.launcher',version='1.7',code=8,
             sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),bytes=apk.stat().st_size,
             signer='7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d',
             same_uid=True,private_fixture_preserved=True,old_contact_preferences_preserved=True,
             native_picker_add_open_remove_and_recreate=True,process_restart_preferences_preserved=True,font_200_controls_reachable=True,real_media_and_ring_index_changes=True,two_column_favourite_tiles=True,old_app_preferences_preserved=True,
             physical_phone_tested=False,hardware_audio_tested=False,play_protect_tested=False,published=False)
(root/'verification.json').write_text(json.dumps(receipt,indent=2)+'\n')
print('::notice title=Exact unpublished Launcher candidate::'+json.dumps(receipt))
PYRECEIPT
