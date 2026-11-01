#!/usr/bin/env python3
"""Pin one signed authorized app and compare actual APK permissions with its public baseline."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from verify_release_permission_delta import parse_badging

SIGNER = '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
APPS = {
    'cleaner': ('com.universalrp.cleansweep', '2.82', 87, 'live-guard-v2.81', 'CleanSweep-v2.81.apk',
                'c851a09f20a0799a49789047d970f2ca512d71a53d16699cd19c36ef57517805', 'CleanSweep-v2.82.apk'),
    'voicenotes': ('com.universalrp.voicenotes', '1.3.4', 8, 'voice-notes-v1.3.3', 'Voice-Notes-v1.3.3.apk',
                   '71aba878bc85419933f6d8050626bcf19056f76c4cac7806e8f899efca404d35', 'Voice-Notes-v1.3.4.apk'),
}

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def output(*args): return subprocess.check_output(args, text=True)

def main(module):
    package, version, code, tag, old_name, old_sha, new_name = APPS[module]
    assert os.environ.get('GITHUB_ACTIONS') == 'true'
    source = os.environ['GITHUB_SHA']; assert re.fullmatch('[a-f0-9]{40}', source)
    root = Path(os.environ['RUNNER_TEMP']) / ('health-' + module); root.mkdir(exist_ok=True)
    baseline = root / old_name
    subprocess.run(['gh', 'release', 'download', tag, '--pattern', old_name, '--dir', str(root)], check=True)
    assert sha(baseline) == old_sha
    original = Path(module) / f'build/outputs/apk/release/{module}-release.apk'
    candidate = root / new_name; candidate.write_bytes(original.read_bytes())
    sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools'
    tools = sorted(sdk.iterdir(), key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.name)))[-1]
    badging = output(str(tools/'aapt2'), 'dump', 'badging', str(candidate))
    previous = output(str(tools/'aapt2'), 'dump', 'badging', str(baseline))
    assert f"package: name='{package}' versionCode='{code}' versionName='{version}'" in badging
    assert "sdkVersion:'26'" in badging and 'application-debuggable' not in badging
    for apk in (candidate, baseline):
        certs = output(str(tools/'apksigner'), 'verify', '--print-certs', str(apk))
        signers = set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)', certs))
        assert signers == {SIGNER}, 'Signer continuity failed'
    permissions = parse_badging(badging); old_permissions = parse_badging(previous)
    assert permissions and permissions == old_permissions, 'Full merged APK permission set changed'
    new_badging = root/'candidate-badging.txt'; old_badging = root/'previous-badging.txt'
    new_badging.write_text(badging); old_badging.write_text(previous)
    delta = output(sys.executable, 'tools/verify_release_permission_delta.py', '--candidate-badging', str(new_badging), '--previous-badging', str(old_badging))
    assert 'EVIDENCE-REQUIRED: no' in delta
    (root/'permission-delta.txt').write_text(delta)
    receipt = dict(source=source, module=module, package=package, version=version, code=code,
                   apk=new_name, sha256=sha(candidate), bytes=candidate.stat().st_size, signer=SIGNER,
                   baseline_tag=tag, baseline_sha256=old_sha, permissions=sorted(permissions),
                   added_permissions=[], removed_permissions=[], physical_phone_tested=False,
                   play_protect_tested=False)
    (root/'verification.json').write_text(json.dumps(receipt, indent=2)+'\n')
    (root/(new_name.removesuffix('.apk')+'.sha256')).write_text(sha(candidate)+'  '+new_name+'\n')
    print('::notice title=Signed candidate identity::'+json.dumps(receipt))

if __name__ == '__main__': main(sys.argv[1])
