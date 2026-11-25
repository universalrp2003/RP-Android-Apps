#!/usr/bin/env python3
"""Disposable-emulator checks only: no publication, data clearing, or uninstall."""
from pathlib import Path
import hashlib
import subprocess
import sys


def one(pattern):
    matches = list(Path('.').glob(pattern))
    if len(matches) != 1:
        raise RuntimeError(f'Expected one {pattern}, found {matches}')
    return matches[0]

apk = one('cleaner/build/outputs/apk/release/*.apk')
test_apk = one('cleaner/build/outputs/apk/androidTest/release/*.apk')
sha = hashlib.sha256(apk.read_bytes()).hexdigest()
for file in (apk, test_apk):
    subprocess.run(['adb', 'install', '-r', str(file)], check=True)


def instrument(test_class, count):
    command = ['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'expectedCandidateSha256', sha,
               '-e', 'class', 'com.universalrp.cleansweep.' + test_class,
               'com.universalrp.cleansweep.test/androidx.test.runner.AndroidJUnitRunner']
    result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(result.stdout)
    if result.returncode != 0 or f'OK ({count} test' not in result.stdout or 'FAILURES!!!' in result.stdout:
        detail = result.stdout[-7000:].replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
        print('::error title=Live Guard native test::' + detail)
        sys.exit('Native review tests failed or did not finish (see output above).')


instrument('LiveGuardReviewNativeTest', 3)
instrument('NewsRotationRestartNativeTest#seedStartedVoice', 1)
# Only the disposable emulator's app process is stopped; no uninstall or data clear.
subprocess.run(['adb', 'shell', 'am', 'force-stop', 'com.universalrp.cleansweep'], check=True)
instrument('NewsRotationRestartNativeTest#readAfterForceStop', 1)
print('::notice title=Live Guard native checks::Three UI/playback checks and saved voice across Android process restart passed; target SHA-256 ' + sha)
print('Not a physical-phone or audible Tamil synthesis test.')
