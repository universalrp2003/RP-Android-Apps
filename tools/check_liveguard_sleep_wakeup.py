#!/usr/bin/env python3
"""CI EMULATOR ONLY. Seeds a short deadline to test the actual release alarm/FGS in Doze.
Not an audio, remote-provider, 30m/1h/2h/3h wall-clock or physical-phone certification.
"""
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = 'com.universalrp.cleansweep'
DATA = f'/data/user/0/{PKG}'
OUT = Path(os.environ['RUNNER_TEMP']) / 'news-wakeup-evidence'
OUT.mkdir(exist_ok=True)
def adb(*args, input=None):
    return subprocess.check_output(['adb', *args], input=input, timeout=40).decode(errors='replace').strip()
def shell(*args): return adb('shell', *args)
def prefs(name):
    raw = shell('cat', f'{DATA}/shared_prefs/{name}.xml')
    return {e.get('name'): e.get('value', e.text) for e in ET.fromstring(raw)}
def seed(name, values):
    root = ET.Element('map')
    for key, value in values.items():
        tag = 'boolean' if isinstance(value, bool) else 'long' if isinstance(value, int) else 'string'
        e = ET.SubElement(root, tag, name=key)
        if tag == 'string': e.text = value
        else: e.set('value', str(value).lower())
    adb('shell', f'cat > {DATA}/shared_prefs/{name}.xml', input=ET.tostring(root))
def main(apk):
    assert os.environ.get('CI') == 'true' and os.environ.get('GITHUB_ACTIONS') == 'true', 'CI emulator only'
    assert adb('get-serialno').startswith('emulator-')
    assert shell('getprop', 'ro.kernel.qemu') == '1'
    adb('root'); adb('wait-for-device')
    assert shell('id', '-u') == '0', 'Release APK prefs require rooted test emulator'
    print(adb('install', '-r', apk))
    uid = shell('stat', '-c', '%u', DATA)
    assert uid.isdigit() and int(uid) >= 10000
    shell('mkdir', '-p', f'{DATA}/shared_prefs')
    now = int(shell('date', '+%s')) * 1000
    # The real 30-minute preference is kept. Only its persisted last-success time is seeded.
    # Due in 90 seconds: enough time to background, kill the process and enter forced Doze.
    due = now + 90_000
    seed('cleansweep_news_voice', {'news_voice_interval_minutes': 30,
        'news_voice_last_auto_ms': due - 30 * 60_000,
        'news_voice_v265_default_3h_on': True})
    seed('cleansweep_state', {'voice_on': True, 'voice_news_headlines': True,
        'persistent_monitor': False, 'charge_monitor': False, 'status_pill': False})
    shell('chown', '-R', f'{uid}:{uid}', f'{DATA}/shared_prefs')
    shell('restorecon', '-RF', f'{DATA}/shared_prefs')
    shell('appops', 'set', PKG, 'SCHEDULE_EXACT_ALARM', 'allow')
    shell('pm', 'grant', PKG, 'android.permission.POST_NOTIFICATIONS')
    adb('logcat', '-c')
    print(shell('am', 'start', '-W', '-n', PKG + '/.MainActivity'))
    scheduled = prefs('liveguard_news_schedule')
    assert int(scheduled['next_check']) == due, scheduled
    shell('input', 'keyevent', 'KEYCODE_HOME')
    shell('input', 'keyevent', 'KEYCODE_SLEEP')
    shell('am', 'kill', PKG)  # NOT force-stop: force-stop legitimately cancels all app alarms/jobs.
    assert not shell('pidof', PKG), 'Process must be gone before the alarm fires'
    shell('dumpsys', 'battery', 'unplug')
    print(shell('dumpsys', 'deviceidle', 'force-idle'))
    idle = shell('dumpsys', 'deviceidle')
    (OUT/'idle-before.txt').write_text(idle)
    assert 'mState=IDLE' in idle, 'Must actually enter deep Doze'
    assert 'mWakefulness=Asleep' in shell('dumpsys', 'power'), 'Screen must be off'
    deadline = time.monotonic() + 130
    observed = None
    while time.monotonic() < deadline:
        observed = prefs('liveguard_news_schedule')
        if int(observed.get('last_check', 0)) >= due - 1000:
            break
        time.sleep(2)
    assert observed and observed.get('last_origin') == 'alarm', observed
    checked = int(observed.get('last_check', 0))
    assert due - 1000 <= checked <= due + 30_000, observed
    assert int(observed['next_check']) > checked, observed
    report = {'source': os.environ['GITHUB_SHA'], 'apk': Path(apk).name,
        'expectedDueMs': due, 'observedCheckMs': checked, 'latenessMs': checked - due,
        'status': observed, 'result': 'PASS: persisted exact alarm restarted a dead app process and entered the foreground check in forced Doze',
        'limits': 'Seeded 90-second deadline; no full real-time interval, provider, completed-audio, OEM or physical-phone proof.'}
    (OUT/'wakeup.json').write_text(json.dumps(report, indent=2))
    print(json.dumps(report, indent=2))

if __name__ == '__main__':
    try: main(sys.argv[1])
    finally:
        for name, command in [('alarm-after.txt', ('dumpsys','alarm')), ('activity-after.txt', ('dumpsys','activity','services',PKG))]:
            try: (OUT/name).write_text(shell(*command))
            except subprocess.SubprocessError: pass
        try: (OUT/'logcat.txt').write_text(adb('logcat','-d','-t','700'))
        except subprocess.SubprocessError: pass
        try: shell('dumpsys','deviceidle','unforce'); shell('dumpsys','battery','reset')
        except subprocess.SubprocessError: pass
