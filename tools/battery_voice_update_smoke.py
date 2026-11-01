#!/usr/bin/env python3
"""Synthetic same-key upgrade fixtures, permitted ONLY on a disposable Actions emulator."""
import hashlib
import json
import os
from pathlib import Path
import re
import runpy
import subprocess
import sys
import time

HELPERS = runpy.run_path('tamilanstore/tools/signed-upgrade-smoke.py', run_name='ci_helpers')
adb = HELPERS['adb']

def main(module):
    assert os.environ.get('CI') == 'true' and os.environ.get('GITHUB_ACTIONS') == 'true'
    assert adb('get-serialno').strip().startswith('emulator-')
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1'
    HELPERS['enable_ci_root']()
    root = Path(os.environ['RUNNER_TEMP']) / ('health-' + module)
    receipt = json.loads((root/'verification.json').read_text())
    package = receipt['package']; candidate = root/receipt['apk']
    assert hashlib.sha256(candidate.read_bytes()).hexdigest() == receipt['sha256']
    old = next(p for p in root.glob('*.apk') if p.name != receipt['apk'])
    assert hashlib.sha256(old.read_bytes()).hexdigest() == receipt['baseline_sha256']
    adb('install', str(old))
    def state():
        dump = adb('shell', 'dumpsys', 'package', package)
        uid = re.search(r'^package:'+re.escape(package)+r'\s+uid:(\d+)\s*$',
                        adb('shell', 'cmd', 'package', 'list', 'packages', '-U', '--user', '0', package), re.M)
        assert uid
        return uid[1], dump
    uid, _ = state()
    data = f'/data/user/0/{package}'
    fixture = root/'synthetic.json'
    if module == 'voicenotes':
        fixture.write_text(json.dumps([dict(id='ci-synthetic', text='Synthetic update note. செயற்கை சோதனை.', createdAtEpochMs=1700000000000)], ensure_ascii=False))
        filename = 'voice-notes.json'
        prefs_name = 'voice-notes'
        pref_xml = '<map><boolean name="hd_voice_disclosure_acknowledged" value="false"/><boolean name="app_lock" value="false"/></map>'
    else:
        fixture.write_text('Synthetic update marker only. No private data.')
        filename = 'synthetic-upgrade-marker'
        prefs_name = 'cleansweep_voice'
        # Announcer's real preference file is resolved below, never guess a name.
        source = Path('cleaner/src/main/java/com/universalrp/cleansweep/voice/Announcer.kt').read_text()
        match = re.search(r'const val PREFS = "([^"]+)"', source); assert match
        prefs_name = match[1]
        pref_xml = '<map><int name="voice_low_percent" value="33"/><boolean name="voice_battery_low" value="false"/></map>'
    shared = root/'synthetic-prefs.xml'; shared.write_text(pref_xml)
    adb('shell', f'mkdir -p {data}/files {data}/shared_prefs')
    adb('push', str(fixture), f'{data}/files/{filename}')
    adb('push', str(shared), f'{data}/shared_prefs/{prefs_name}.xml')
    adb('shell', f'chown -R {uid}:{uid} {data}/files {data}/shared_prefs; restorecon -RF {data}/files {data}/shared_prefs')
    before = adb('shell', 'sha256sum', f'{data}/files/{filename}').split()[0]
    before_prefs = adb('shell', 'sha256sum', f'{data}/shared_prefs/{prefs_name}.xml').split()[0]
    # This is the tested operation: normal Android update, no uninstall/clear/-d/-t/bypass.
    adb('install', '-r', str(candidate))
    new_uid, dump = state()
    assert new_uid == uid and f'versionCode={receipt["code"]}' in dump and f'versionName={receipt["version"]}' in dump
    assert adb('shell', 'sha256sum', f'{data}/files/{filename}').split()[0] == before
    assert adb('shell', 'sha256sum', f'{data}/shared_prefs/{prefs_name}.xml').split()[0] == before_prefs
    adb('shell', 'am', 'start', '-W', '-n', package+'/.MainActivity')
    time.sleep(3)
    assert adb('shell', 'pidof', package).strip()
    assert adb('shell', 'cat', f'{data}/files/{filename}').strip() == fixture.read_text().strip()
    adb('shell', 'am', 'force-stop', package)
    adb('shell', 'am', 'start', '-W', '-n', package+'/.MainActivity')
    time.sleep(2)
    assert adb('shell', 'pidof', package).strip()
    assert adb('shell', 'cat', f'{data}/files/{filename}').strip() == fixture.read_text().strip()
    if module == 'voicenotes':
        test_apks = list(Path(module+'/build/outputs/apk/androidTest/release').glob('*.apk')); assert len(test_apks) == 1
        adb('install', str(test_apks[0]))
        result = adb('shell', 'am', 'instrument', '-w', '-r', package+'.test/androidx.test.runner.AndroidJUnitRunner', timeout=240)
        (root/'instrumentation.txt').write_text(result)
        print(result)
        assert 'OK (2 tests)' in result and 'FAILURES' not in result and 'INSTRUMENTATION_FAILED' not in result
        # Prove the actual installed target is still the exact release, not a debug rebuild.
        installed = adb('shell', 'pm', 'path', package).strip().removeprefix('package:')
        pulled = root/'installed-exact.apk'; adb('pull', installed, str(pulled))
        assert hashlib.sha256(pulled.read_bytes()).hexdigest() == receipt['sha256']
        pulled.unlink()
    receipt.update(same_uid=True, data_preserved=True, preferences_preserved=True, restart_passed=True,
                   update_method='adb install -r; no data clear, uninstall or bypass',
                   real_hd_playback_passed=module == 'voicenotes')
    (root/'verification.json').write_text(json.dumps(receipt, indent=2)+'\n')
    print('::notice title=Exact signed upgrade and playback::'+json.dumps(receipt))

if __name__ == '__main__': main(sys.argv[1])
