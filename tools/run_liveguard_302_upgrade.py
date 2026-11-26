#!/usr/bin/env python3
"""Run a same-signer in-place update on a disposable CI emulator; never clear or uninstall."""
from pathlib import Path
import hashlib
import os
import re
import subprocess
import sys

pkg = 'com.universalrp.cleansweep'
baseline = Path('/tmp/liveguard302-baseline.apk')
expected_baseline = os.environ['BASELINE_SHA']
candidates = list(Path('cleaner/build/outputs/apk/release').glob('*.apk'))
tests = list(Path('cleaner/build/outputs/apk/androidTest/release').glob('*.apk'))
assert len(candidates) == len(tests) == 1
candidate, test_apk = candidates[0], tests[0]
digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
assert digest(baseline) == expected_baseline, 'Baseline digest mismatch'
expected_candidate = Path('/tmp/liveguard302-candidate/sha256.txt').read_text().strip()
assert re.fullmatch('[a-f0-9]{64}', expected_candidate) and digest(candidate) == expected_candidate


def instrument(test_class):
    cmd = ['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'expectedCandidateSha256', expected_candidate,
           '-e', 'expectedBaselineCode', '106', '-e', 'expectedCandidateCode', '107',
           '-e', 'class', 'com.universalrp.cleansweep.' + test_class,
           'com.universalrp.cleansweep.test/androidx.test.runner.AndroidJUnitRunner']
    p = subprocess.run(cmd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(p.stdout, flush=True)
    if p.returncode or 'OK (1 test' not in p.stdout or 'FAILURES!!!' in p.stdout:
        print('::error title=3.02 upgrade instrumentation::' + p.stdout[-7000:].replace('%','%25').replace('\r','%0D').replace('\n','%0A'))
        sys.exit('Upgrade test failed')


def uid():
    output = subprocess.check_output(['adb', 'shell', 'pm', 'list', 'packages', '-U', '--user', '0', pkg], text=True)
    values = re.findall(r'^package:' + re.escape(pkg) + r'\s+uid:(\d+)\s*$', output, flags=re.MULTILINE)
    assert len(values) == 1, output
    return values[0]

subprocess.run(['adb', 'install', '-r', str(baseline)], check=True)
subprocess.run(['adb', 'install', '-r', str(test_apk)], check=True)
before = uid()
instrument('CandidateUpgradeSeedNativeTest')
subprocess.run(['adb', 'install', '-r', str(candidate)], check=True)
assert before == uid(), 'Android UID changed'
instrument('CandidateUpgradeVerifyNativeTest')
print('::notice title=3.02 upgrade::Same UID, saved choices and old news dedup preserved; baseline ' + expected_baseline + ', candidate ' + expected_candidate)
subprocess.run(['python3', 'tools/run_liveguard_review_native.py'], check=True)
