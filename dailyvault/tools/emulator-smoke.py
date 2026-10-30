#!/usr/bin/env python3
"""Release-APK emulator checks with synthetic data only. No owner records/passwords.
Exercises fresh opening, expenses/notes, restart persistence, legacy one-time recovery,
independent backup password, wrong-password safety, restore and screenshot availability.
"""
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = 'com.universalrp.dailyvault'
OUT = Path('dailyvault-emulator-results'); OUT.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output(['adb', *args], timeout=40).decode(errors='replace')

def launch():
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    time.sleep(2)

def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/dailyvault-ui.xml')
    text = adb('shell', 'cat', '/sdcard/dailyvault-ui.xml')
    (OUT / 'last-ui.xml').write_text(text)
    return list(ET.fromstring(text).iter('node'))

def wait_text(text, timeout=35):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        try:
            ns = nodes()
            if any(text.casefold() in (n.get('text', '') + n.get('content-desc', '')).casefold() for n in ns): return ns
        except Exception:
            pass
        time.sleep(.5)
    raise AssertionError('UI text not found: ' + text)

def point(n):
    x1,y1,x2,y2 = map(int, re.findall(r'\d+', n.get('bounds', '')))
    return str((x1+x2)//2), str((y1+y2)//2)

def tap(text):
    ns = wait_text(text)
    found = [n for n in ns if n.get('text', '').casefold() == text.casefold() or n.get('content-desc', '').casefold() == text.casefold()]
    assert found, 'Exact clickable label not found: ' + text
    adb('shell', 'input', 'tap', *point(found[-1]))
    time.sleep(.6)

def field(index, value, clear=False):
    editable = [n for n in nodes() if n.get('class') == 'android.widget.EditText']
    assert index < len(editable), f'Missing text field {index}; found {len(editable)}'
    adb('shell', 'input', 'tap', *point(editable[index]))
    if clear:
        adb('shell', 'input', 'keyevent', '123')
        adb('shell', 'input', 'keyevent', *(['67'] * 256))
    adb('shell', 'input', 'text', value.replace(' ', '%s'))
    time.sleep(.4)

def keyboard_back():
    adb('shell', 'input', 'keyevent', '4'); time.sleep(.4)

def shot(name):
    path = OUT / (name + '.png')
    path.write_bytes(subprocess.check_output(['adb', 'exec-out', 'screencap', '-p'], timeout=30))
    assert path.read_bytes().startswith(b'\x89PNG') and path.stat().st_size > 15_000, 'Screenshot unavailable/blank'

def english():
    if any(n.get('text') == 'English' for n in nodes()): tap('English')

def private_hash():
    return adb('shell', 'sha256sum', f'/data/user/0/{PACKAGE}/files/daily-vault.tdv').split()[0]

def main():
    apk = sys.argv[1]
    adb('install', '-r', apk)
    launch(); english(); wait_text('A fresh start'); shot('01-fresh-overview-en')
    tap('Add spending'); wait_text('Spending record')
    field(0, '123.45'); field(1, 'Smoke groceries'); keyboard_back(); tap('Save')
    wait_text('Record saved'); wait_text('Smoke groceries'); shot('02-overview-with-spending')
    tap('Records'); tap('Notes / history'); tap('Add note')
    field(0, 'Smoke note'); keyboard_back(); tap('Save')
    wait_text('Smoke note')
    adb('shell', 'am', 'force-stop', PACKAGE); launch(); english(); tap('Overview'); wait_text('Smoke groceries'); wait_text('Smoke note')
    # No app-opening password should appear after a process restart.
    assert not any(n.get('class') == 'android.widget.EditText' for n in nodes())
    tap('தமிழ்'); wait_text('தினக்கணக்கு'); shot('03-tamil-overview')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.3'); time.sleep(2); shot('04-large-font-tamil')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0'); time.sleep(2)
    english()
    # Replace only this emulator's synthetic local file with the independent old TDV-v1 fixture.
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('root'); adb('wait-for-device')
    uid = re.search(r'uid:(\d+)', adb('shell', 'pm', 'list', 'packages', '-U', PACKAGE)).group(1)
    target = f'/data/user/0/{PACKAGE}'
    adb('push', 'dailyvault/src/test/resources/legacy-v1.tdv', '/data/local/tmp/dailyvault-legacy.tdv')
    adb('shell', 'sh', '-c', f'"cp /data/local/tmp/dailyvault-legacy.tdv {target}/files/daily-vault.tdv; chmod 600 {target}/files/daily-vault.tdv; chown {uid}:{uid} {target}/files/daily-vault.tdv; rm -f {target}/shared_prefs/local-unlock.xml; restorecon -R {target}"')
    original = hashlib.sha256(Path('dailyvault/src/test/resources/legacy-v1.tdv').read_bytes()).hexdigest()
    assert private_hash() == original
    launch(); english(); wait_text('Recover previous records'); tap('Recover old vault')
    field(0, 'WrongPassword123'); keyboard_back(); tap('Recover'); wait_text('Check the password or file')
    assert private_hash() == original, 'Wrong recovery password changed the old vault'
    field(0, 'LegacyBackupTest123', clear=True); keyboard_back(); tap('Recover')
    wait_text('Legacy groceries'); wait_text('Legacy reminder')
    assert private_hash() == original, 'Legacy recovery should preserve authenticated file bytes'
    adb('shell', 'am', 'force-stop', PACKAGE); launch(); english(); wait_text('Legacy groceries')
    tap('Backup'); shot('05-backup-screen')
    tap('Export encrypted backup'); tap('Save')
    wait_text('Choose a backup password')
    field(0, 'IndependentBackup987'); field(1, 'IndependentBackup987'); keyboard_back(); tap('Save backup')
    wait_text('Backup file written')
    paths = adb('shell', 'find', '/storage/emulated/0/Download', '-name', 'DailyVault-*.tdv').splitlines()
    assert paths, 'SAF export file not found in Downloads'
    backup = paths[-1]; adb('pull', backup, str(OUT / 'synthetic-backup.tdv'))
    content = (OUT / 'synthetic-backup.tdv').read_bytes()
    assert content[:4] == b'TDV\x01' and b'Legacy groceries' not in content
    tap('Overview'); tap('Add spending'); field(0, '1.00'); field(1, 'Temporary record'); keyboard_back(); tap('Save'); wait_text('Temporary record')
    tap('Backup'); tap('Restore from file'); tap(Path(backup).name)
    wait_text('Restore a backup')
    field(0, 'WrongPassword123'); keyboard_back()
    checkbox = next(n for n in nodes() if n.get('class') == 'android.widget.CheckBox')
    adb('shell', 'input', 'tap', *point(checkbox))
    before = private_hash(); tap('Replace and restore'); wait_text('Check the password or file')
    assert private_hash() == before, 'Wrong backup password replaced current records'
    field(0, 'IndependentBackup987', clear=True); keyboard_back(); tap('Replace and restore')
    wait_text('Records restored from backup'); wait_text('Legacy groceries')
    assert not any('Temporary record' in n.get('text', '') for n in nodes())
    shot('06-restored-overview')
    tap('Records'); tap('Notes / history'); wait_text('Legacy reminder'); shot('07-restored-notes')
    tap('Backup'); tap('தமிழ்'); shot('08-backup-tamil')
    (OUT / 'PASS.txt').write_text('PASS: exact signed release APK; fresh no-login opening; expense/note saves; process restart; Tamil; screenshot capture; 1.3 font capture; legacy one-time recovery; wrong-password file preservation; password-protected export; independent-password restore. Emulator only, synthetic data; not physical-device/Play Protect certification.\n')
    # Do not retain the synthetic backup/password as user-facing report artifacts.
    (OUT / 'synthetic-backup.tdv').unlink(missing_ok=True)
    print((OUT / 'PASS.txt').read_text())

if __name__ == '__main__':
    try: main()
    except Exception:
        try: shot('failure')
        except Exception: pass
        subprocess.run(['adb', 'logcat', '-d', '-t', '400'], stdout=(OUT / 'emulator-log.txt').open('w'), timeout=30)
        raise
