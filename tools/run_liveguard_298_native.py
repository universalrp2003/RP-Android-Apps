#!/usr/bin/env python3
"""Run native tests with a mandatory in-test hash of the exact archived APK."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def main():
    assert os.environ.get('GITHUB_ACTIONS') == os.environ.get('CI') == 'true'
    root = Path(os.environ['RUNNER_TEMP'])/'health-cleaner'
    r = json.loads((root/'verification.json').read_text())
    digest = r['sha256']
    assert re.fullmatch('[a-f0-9]{64}',digest)
    for apk in (root/r['apk'], Path('cleaner/build/outputs/apk/release/cleaner-release.apk')):
        assert apk.stat().st_size == r['bytes']
        assert hashlib.sha256(apk.read_bytes()).hexdigest() == digest
    assert r['final_installed_apk_sha256'] == digest
    subprocess.run(['gradle',':cleaner:connectedReleaseAndroidTest',
        '-Pandroid.testInstrumentationRunnerArguments.expectedCandidateSha256='+digest,
        '--no-daemon','--stacktrace'],check=True)

if __name__ == '__main__':
    main()
