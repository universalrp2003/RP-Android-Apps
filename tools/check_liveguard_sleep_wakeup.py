#!/usr/bin/env python3
"""CI EMULATOR ONLY. Seeds a short deadline to test the actual release alarm/FGS in Doze.
Not an audio, remote-provider, 30m/1h/2h/3h wall-clock or physical-phone certification.
"""
import json
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import time
import traceback
import xml.etree.ElementTree as ET

PKG = 'com.universalrp.cleansweep'
DATA = f'/data/user/0/{PKG}'
OUT = Path(os.environ['RUNNER_TEMP']) / 'news-wakeup-evidence'
OUT.mkdir(exist_ok=True)
def report_failure(kind, error, tb):
    detail = ''.join(traceback.format_exception(kind, error, tb))[-1800:]
    print('::error title=Sleeping news emulator::' + detail.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A'))
    # Keep failures inspectable through the GitHub Checks API even when artifact/log
    # storage is unavailable. Never relax delivery, dead-process or timing assertions.
    for name in ('alarm-after.txt', 'activity-after.txt', 'power-after.txt', 'idle-before.txt', 'logcat.txt'):
        report = OUT / name
        if not report.exists(): continue
        lines = report.read_text(errors='replace').splitlines()
        if name in ('alarm-after.txt', 'activity-after.txt', 'logcat.txt'):
            matches = set()
            for index, line in enumerate(lines):
                if PKG in line or 'AndroidRuntime' in line or 'FATAL EXCEPTION' in line:
                    matches.update(range(max(0, index-2), min(len(lines), index+8)))
            lines = [lines[index] for index in sorted(matches)]
        detail = '\n'.join(lines)[-5000:]
        print('::warning title=Sleeping news diagnostic::' + (name + '\n' + detail).replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A'))

sys.excepthook = report_failure
def adb(*args, input=None):
    return subprocess.check_output(['adb', *args], input=input, timeout=40).decode(errors='replace').strip()
def shell(*args): return adb('shell', *args)
def prefs(name):
    raw = shell('cat', f'{DATA}/shared_prefs/{name}.xml')
    return {e.get('name'): e.get('value', e.text) for e in ET.fromstring(raw)}
def seed(name, values):
    root = ET.Element('map')
    for key, value in values.items():
        tag = 'boolean' if isinstance(value, bool) else 'int' if key == 'news_voice_interval_minutes' else 'long' if isinstance(value, int) else 'string'
        e = ET.SubElement(root, tag, name=key)
        if tag == 'string': e.text = value
        else: e.set('value', str(value).lower())
    adb('shell', f'cat > {DATA}/shared_prefs/{name}.xml', input=ET.tostring(root))
def main(apk):
    assert os.environ.get('CI') == 'true' and os.environ.get('GITHUB_ACTIONS') == 'true', 'CI emulator only'
    assert adb('get-serialno').startswith('emulator-')
    assert shell('getprop', 'ro.kernel.qemu') == '1'
    for attempt in range(3):
        try:
            adb('root'); adb('wait-for-device')
            if shell('id', '-u') == '0': break
        except subprocess.SubprocessError:
            if attempt == 2: raise
        time.sleep(2)
    assert shell('id', '-u') == '0', 'Release APK prefs require rooted test emulator'
    print(adb('install', '-r', apk))
    uid = shell('stat', '-c', '%u', DATA)
    assert uid.isdigit() and int(uid) >= 10000
    shell('mkdir', '-p', f'{DATA}/shared_prefs', f'{DATA}/files')
    # SettingsRepo's DataStore is authoritative; a plain-prefs mirror alone would be
    # overwritten by its default-ON monitor flows during first launch. Encode only the
    # two bool entries using androidx PreferencesProto's documented map/Value schema.
    encoded = b''
    for key in ('charge_monitor', 'persistent_monitor'):
        key = key.encode()
        entry = bytes([10, len(key)]) + key + bytes([18, 2, 8, 0])
        encoded += bytes([10, len(entry)]) + entry
    adb('shell', f'cat > {DATA}/files/cleansweep_settings.preferences_pb', input=encoded)
    shell('chown', '-R', f'{uid}:{uid}', f'{DATA}/files')
    shell('restorecon', '-RF', f'{DATA}/files')
    # Unplug while the freshly installed app is still stopped; do not send a power
    # broadcast after proving its process dead, which could itself restart the app.
    shell('dumpsys', 'battery', 'unplug')
    now = int(shell('date', '+%s')) * 1000
    # The real 30-minute preference is kept. Only its persisted last-success time is seeded.
    # Due in 90 seconds: enough time to background, kill the process and enter forced Doze.
    due = now + 90_000
    seed('cleansweep_news_voice', {'news_voice_interval_minutes': 30,
        'news_voice_last_auto_ms': due - 30 * 60_000,
        'news_voice_v265_default_3h_on': True})
    seed('cleansweep_net_split', {'split_enabled': False, 'radio_change_announce': False})
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
    # Startup workers can briefly bind JobService. Wait only for quiescence; never
    # force-stop (that legitimately cancels alarms). pidof exits 1 when successfully dead.
    for _ in range(15):
        shell('am', 'kill', PKG)
        if not adb('shell', f'pidof {PKG} || true'): break
        time.sleep(2)
    assert not adb('shell', f'pidof {PKG} || true'), 'Process must be gone before the alarm fires'
    print(shell('dumpsys', 'deviceidle', 'force-idle'))
    idle = shell('dumpsys', 'deviceidle')
    (OUT/'idle-before.txt').write_text(idle)
    assert 'mState=IDLE' in idle, 'Must actually enter deep Doze'
    assert 'mWakefulness=Asleep' in shell('dumpsys', 'power'), 'Screen must be off'
    assert not adb('shell', f'pidof {PKG} || true'), 'No other broadcast/job may restart the app before the alarm'
    assert int(shell('date', '+%s')) * 1000 < due, 'Fixture setup must finish before the seeded deadline'
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
    identity = json.loads((Path(apk).parent / 'candidate-info.json').read_text())
    assert identity['source'] == os.environ['GITHUB_SHA'], identity
    assert identity['sha256'] == hashlib.sha256(Path(apk).read_bytes()).hexdigest(), 'Tested APK differs from retained signed candidate'
    report = {'source': os.environ['GITHUB_SHA'], 'apk': Path(apk).name, 'candidate': identity,
        'expectedDueMs': due, 'observedCheckMs': checked, 'latenessMs': checked - due,
        'status': observed, 'result': 'PASS: persisted exact alarm restarted a dead app process and entered the foreground check in forced Doze',
        'limits': 'Seeded 90-second deadline; no full real-time interval, provider, completed-audio, OEM or physical-phone proof.'}
    (OUT/'wakeup.json').write_text(json.dumps(report, indent=2))
    print(json.dumps(report, indent=2))
    print('::notice title=Sleeping news verified::' + json.dumps(report).replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A'))

if __name__ == '__main__':
    try: main(sys.argv[1])
    finally:
        for name, command in [('alarm-after.txt', ('dumpsys','alarm')), ('activity-after.txt', ('dumpsys','activity','services',PKG)), ('power-after.txt', ('dumpsys','power'))]:
            try: (OUT/name).write_text(shell(*command))
            except subprocess.SubprocessError: pass
        try: (OUT/'logcat.txt').write_text(adb('logcat','-d','-t','700'))
        except subprocess.SubprocessError: pass
        try: shell('dumpsys','deviceidle','unforce'); shell('dumpsys','battery','reset')
        except subprocess.SubprocessError: pass
