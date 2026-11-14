#!/usr/bin/env bash
# Disposable stock emulator only: install the pinned release APK, launch and navigate without Shizuku.
# This is NOT an OEM compatibility test and does NOT exercise disable/suspend/enable.
set -euo pipefail
apk=${1:?pass the pinned APK path}
expected=${2:?pass the expected APK SHA-256}
evidence=${3:?pass an evidence directory}
mkdir -p "$evidence"
case "$(adb get-serialno)" in emulator-*) ;; *) echo 'Refusing to run outside an emulator' >&2; exit 1;; esac
printf '%s  %s\n' "$expected" "$apk" | sha256sum -c -
adb shell getprop ro.build.version.sdk | tr -d '\r' | tee "$evidence/api.txt"
adb shell getprop ro.build.fingerprint | tr -d '\r' | tee "$evidence/fingerprint.txt"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb logcat -c
adb install -r "$apk"
adb shell am start -W -n com.universalrp.appdisabler/.ui.MainActivity | tee "$evidence/launch.txt"
adb shell pidof com.universalrp.appdisabler | tee "$evidence/pid.txt"
# UIAutomator can transiently fail to find an idle window during emulator startup.
dump_ui() {
    local destination=$1
    local attempt
    for attempt in 1 2 3 4; do
        if adb shell uiautomator dump /sdcard/appdisabler-smoke.xml &&
            adb exec-out cat /sdcard/appdisabler-smoke.xml > "$destination" &&
            python3 -c 'import sys, xml.etree.ElementTree as E; E.parse(sys.argv[1])' "$destination"; then
            return 0
        fi
        echo "UI dump attempt $attempt failed; retrying" >&2
        sleep 5
    done
    echo 'Could not capture the UI hierarchy' >&2
    return 1
}
sleep 8
dump_ui "$evidence/setup.xml"
python3 - "$evidence/setup.xml" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter('node'))
texts = [n.attrib.get('text', '') for n in nodes]
assert any('System App Disabler' in t for t in texts), f'App title not visible: {texts}'
assert any('Shizuku' in t for t in texts), f'Shizuku setup not visible: {texts}'
buttons = [n for n in nodes if n.attrib.get('text') == 'Apps']
assert buttons, f'Apps tab not visible: {texts}'
bounds = buttons[0].attrib['bounds']
x1, y1, x2, y2 = map(int, re.findall(r'\d+', bounds))
subprocess.run(['adb', 'shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2)], check=True)
PY
sleep 8
dump_ui "$evidence/apps.xml"
python3 - "$evidence/apps.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter('node'))
texts = [n.attrib.get('text', '') for n in nodes]
assert 'installed' in texts, f'Apps list summary not visible: {texts}'
assert 'pre-installed' in texts, f'Apps list summary not visible: {texts}'
assert any('Shizuku' in t for t in texts), f'No Shizuku guidance on Apps screen: {texts}'
PY
adb shell pidof com.universalrp.appdisabler > "$evidence/pid-after-navigation.txt"
adb exec-out screencap -p > "$evidence/apps.png"
adb logcat -d -v brief -s AndroidRuntime:E > "$evidence/android-runtime.log"
python3 - "$evidence/android-runtime.log" <<'PY'
from pathlib import Path
import sys
log = Path(sys.argv[1]).read_text(errors='replace')
assert 'Process: com.universalrp.appdisabler,' not in log, 'App fatal exception found in logcat'
PY
# Verify that the installed base APK is byte-identical to the selected candidate.
installed=$(adb shell pm path com.universalrp.appdisabler | tr -d '\r' | sed -n 's/^package://p')
test "$(printf '%s\n' "$installed" | wc -l)" -eq 1
adb pull "$installed" "$evidence/installed.apk" >/dev/null
printf '%s  %s\n' "$expected" "$evidence/installed.apk" | sha256sum -c -
sha256sum "$evidence/installed.apk" > "$evidence/installed-sha256.txt"
rm "$evidence/installed.apk"
echo 'PASS: stock emulator launch and Apps navigation without Shizuku. Disable/enable is not tested.' | tee "$evidence/result.txt"
