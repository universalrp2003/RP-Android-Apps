#!/usr/bin/env python3
"""Disposable emulator only: public 3.00 -> exact candidate, preserving synthetic data."""
import hashlib
import os
from pathlib import Path
import re
import subprocess
import tempfile

assert os.environ.get('GITHUB_ACTIONS') == 'true'
assert subprocess.check_output(['adb','get-serialno'], text=True).strip().startswith('emulator-')

def run(*args):
    return subprocess.check_output(args, text=True).strip()

def adb(*args):
    return run('adb', *args)

package = 'com.universalrp.cleansweep'
assert adb('shell','getprop','ro.kernel.qemu') == '1'
root = Path(os.environ['RUNNER_TEMP'])/'candidate'
candidate = root/'Live-Guard-3.01-candidate.apk'
digest = hashlib.sha256(candidate.read_bytes()).hexdigest()
with tempfile.TemporaryDirectory() as tmp:
    run('gh','release','download','live-guard-v3.00','--repo','universalrp2003/RP-Android-Apps',
        '--pattern','CleanSweep-v3.00.apk','--dir',tmp)
    old = Path(tmp)/'CleanSweep-v3.00.apk'
    assert hashlib.sha256(old.read_bytes()).hexdigest() == '286f172054f0a9b10a16d673f49acff1a3bac3d8b3686c9ddf65c27b2b2a5940'
    assert 'Success' in adb('install',str(old))
    adb('root'); adb('wait-for-device')
    line = adb('shell','cmd','package','list','packages','-U','--user','0',package)
    uid = re.search(r'uid:(\d+)',line).group(1)
    base = '/data/user/0/'+package
    fixture = Path(tmp)/'synthetic-upgrade-marker'; fixture.write_text('synthetic data only\n')
    adb('push',str(fixture),'/data/local/tmp/synthetic-upgrade-marker')
    adb('shell','mkdir','-p',base+'/files')
    adb('shell','cp','/data/local/tmp/synthetic-upgrade-marker',base+'/files/synthetic-upgrade-marker')
    adb('shell','chown','-R',f'{uid}:{uid}',base+'/files')
    adb('shell','restorecon','-RF',base+'/files')
    assert 'Success' in adb('install','-r',str(candidate))
    assert re.search(r'uid:(\d+)',adb('shell','cmd','package','list','packages','-U','--user','0',package)).group(1) == uid
    assert adb('shell','cat',base+'/files/synthetic-upgrade-marker') == 'synthetic data only'
    path = adb('shell','pm','path',package).removeprefix('package:')
    assert adb('shell','sha256sum',path).split()[0] == digest
    assert 'versionCode=106' in adb('shell','dumpsys','package',package)
    print('::notice title=3.00 upgrade::Same UID, synthetic private data and exact signed 3.01 APK preserved; no uninstall or clear.')
