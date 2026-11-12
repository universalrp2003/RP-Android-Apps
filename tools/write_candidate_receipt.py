#!/usr/bin/env python3
"""Inspect one real candidate APK. This does not publish or attest phone testing."""
from pathlib import Path
import hashlib
import os
import re
import subprocess
import sys
from verify_browser_update_policy import verify_apk_identity

ROOT = Path(__file__).resolve().parents[1]

def main(module):
    if module not in ('launcher', 'podcast', 'vasanthi', 'radio', 'equalizer'):
        raise ValueError('Unexpected candidate module')
    apks = list((ROOT / module / 'build/outputs/apk/release').glob('*.apk'))
    if len(apks) != 1:
        raise ValueError('Expected exactly one candidate APK; refusing zero or ambiguous matches')
    apk = apks[0]
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or '')
    tools = list((sdk / 'build-tools').glob('*/aapt'))
    if not tools:
        raise ValueError('Android build tools required; no synthetic receipts')
    aapt = sorted(tools, key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.parent.name)))[-1]
    subprocess.run([str(aapt.parent / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], check=True)
    badging = subprocess.check_output([str(aapt), 'dump', 'badging', str(apk)], text=True)
    identity = verify_apk_identity(badging, (ROOT / module / 'build.gradle.kts').read_text())
    source = os.environ.get('GITHUB_SHA', '')
    if not re.fullmatch('[a-f0-9]{40}', source):
        raise ValueError('A real CI source SHA is required')
    digest = hashlib.sha256()
    with apk.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    receipt = '\n'.join(f'{key}: {value}' for key, value in identity.items())
    receipt += f'\nSource: {source}\nFile: {apk.name}\nBytes: {apk.stat().st_size}\nSHA-256: {digest.hexdigest()}\nCandidate only; not a published release or a Play Protect assessment.\n'
    (apk.parent / 'candidate-info.txt').write_text(receipt)
    print(receipt)

if __name__ == '__main__':
    main(sys.argv[1])
