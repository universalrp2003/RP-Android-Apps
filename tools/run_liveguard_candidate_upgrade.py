#!/usr/bin/env python3
"""Pinned candidate upgrade checks on a disposable emulator only; no publication or data clearing."""
from pathlib import Path
import hashlib
import re
import subprocess
import sys

PACKAGE = 'com.universalrp.cleansweep'
BASELINE = Path('/tmp/liveguard-baseline/CleanSweep-v3.00.apk')
BASELINE_SHA = '286f172054f0a9b10a16d673f49acff1a3bac3d8b3686c9ddf65c27b2b2a5940'
CANDIDATE_SHA = '728aa70bcb207b136181af8ecc844faae62a46fa5cbb31172736c1a61bad29d5'


def one(pattern):
    matches = list(Path('.').glob(pattern))
    if len(matches) != 1:
        raise RuntimeError(f'Expected one {pattern}, got {matches}')
    return matches[0]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def instrument(name):
    cmd = ['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'expectedCandidateSha256', CANDIDATE_SHA,
           '-e', 'class', 'com.universalrp.cleansweep.' + name,
           'com.universalrp.cleansweep.test/androidx.test.runner.AndroidJUnitRunner']
    result = subprocess.run(cmd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(result.stdout, flush=True)
    if result.returncode or 'OK (1 test' not in result.stdout or 'FAILURES!!!' in result.stdout:
        detail = result.stdout[-7000:].replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
        print('::error title=Live Guard upgrade test::' + detail)
        sys.exit('Upgrade instrumentation did not pass')


def uid():
    output = subprocess.check_output(['adb', 'shell', 'pm', 'list', 'packages', '-U', '--user', '0', PACKAGE], text=True)
    matches = re.findall(r'^package:' + re.escape(PACKAGE) + r'\s+uid:(\d+)\s*$', output, flags=re.MULTILINE)
    if len(matches) != 1:
        raise RuntimeError('Could not read exact Android package UID: ' + output)
    return matches[0]


candidate = one('cleaner/build/outputs/apk/release/*.apk')
test_apk = one('cleaner/build/outputs/apk/androidTest/release/*.apk')
assert sha(BASELINE) == BASELINE_SHA, 'Published baseline digest mismatch'
assert sha(candidate) == CANDIDATE_SHA, 'Candidate artifact digest mismatch'
subprocess.run(['adb', 'install', '-r', str(BASELINE)], check=True)
subprocess.run(['adb', 'install', '-r', str(test_apk)], check=True)
original_uid = uid()
instrument('CandidateUpgradeSeedNativeTest')
subprocess.run(['adb', 'install', '-r', str(candidate)], check=True)
assert uid() == original_uid, 'UID changed across the in-place update'
instrument('CandidateUpgradeVerifyNativeTest')
print('::notice title=Live Guard candidate upgrade::Published 3.00 updated in place to exact 3.01 candidate; UID, selected saved choices and old news dedup history preserved; no old stories counted as completed. SHA-256 ' + CANDIDATE_SHA)
# Run the full existing native screen/playback and permission checks on these same candidate bytes.
subprocess.run(['python3', 'tools/run_liveguard_review_native.py'], check=True)
