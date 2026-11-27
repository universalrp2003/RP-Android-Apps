#!/usr/bin/env python3
"""Publish only the tested, owner-authorized 0.5.1 candidate; never rebuild or replace."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

REPO = 'universalrp2003/RP-Android-Apps'
TAG = 'system-app-disabler-v0.5.1'
NAME = 'System-App-Disabler-v0.5.1.apk'


def cmd(*args):
    return subprocess.check_output(args, text=True).strip()


def api(path):
    return json.loads(cmd('gh', 'api', f'repos/{REPO}/{path}'))


def main():
    p = json.loads(Path('docs/approved-appdisabler-0.5.1.json').read_text())
    assert os.environ['GITHUB_REF'] == 'refs/heads/arena/30198928-rp-android-apps' and p['owner_authorized'] is True
    run = api(f'actions/runs/{p["run_id"]}')
    assert (run['head_sha'], run['head_branch'], run['conclusion'], run['run_attempt'], run['path']) == (p['source'], 'arena/30198928-rp-android-apps', 'success', p['run_attempt'], '.github/workflows/appdisabler-signed-tv-candidate.yml')
    subprocess.run(['git', 'merge-base', '--is-ancestor', p['source'], 'HEAD'], check=True)
    subprocess.run(['git', 'diff', '--exit-code', p['source'], 'HEAD', '--', 'appdisabler/', 'shared/', 'gradle/', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties'], check=True)
    job, = [j for j in api(f'actions/runs/{p["run_id"]}/jobs')['jobs'] if j['name'] == 'candidate']
    assert job['conclusion'] == 'success'
    for name in ('Download and verify the public 0.5.0 baseline', 'Build signed candidate, instrumentation and release tests', 'Verify version, signer, release build and required unit tests', 'Upgrade public 0.5.0 and test exact signed bytes on disposable TV', 'Create candidate receipt'):
        assert next(s for s in job['steps'] if s['name'] == name)['conclusion'] == 'success', name
    a = api(f'actions/artifacts/{p["artifact_id"]}')
    assert a['workflow_run']['id'] == p['run_id'] and a['workflow_run']['head_sha'] == p['source'] and not a['expired']
    assert a['name'] == 'appdisabler-0.5.1-stable-signed-tested-candidate' and a['digest'] == p['artifact_digest']
    root = Path('/tmp/appdisabler051-publish')
    original = root/'System-App-Disabler-0.5.1-signed-candidate.apk'
    data = original.read_bytes()
    sha = hashlib.sha256(data).hexdigest()
    assert 1_000_000 < len(data) < 80_000_000 and sha == p['apk_sha256']
    assert (root/'System-App-Disabler-0.5.1-signed-candidate.sha256').read_text().split()[0] == sha
    proof = json.loads((root/'verification.json').read_text())
    assert (proof['package'], proof['versionName'], proof['versionCode'], proof['apkSha256'], proof['apkBytes'], proof['sourceCommit'], int(proof['workflowRun']), proof['exactCandidateInstalledAndTested'], proof['published']) == ('com.universalrp.appdisabler', '0.5.1', 7, sha, len(data), p['source'], p['run_id'], True, False)
    apk = root/NAME
    apk.write_bytes(data)
    tools = sorted((Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(), key=lambda d: tuple(map(int, re.findall(r'\d+', d.name))))[-1]
    badge = cmd(str(tools/'aapt'), 'dump', 'badging', str(apk))
    assert "package: name='com.universalrp.appdisabler' versionCode='7' versionName='0.5.1'" in badge and 'application-debuggable' not in badge
    cert = cmd(str(tools/'apksigner'), 'verify', '--print-certs', str(apk))
    assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)', cert)) == {p['signer']}
    release = api('releases/'+str(p['draft_release_id']))
    assert release['tag_name'] == TAG and release['target_commitish'] == p['source'] and release['draft'] and not release['prerelease']
    assert release['body'].strip() == Path('releases/system-app-disabler-v0.5.1.md').read_text().strip()
    assert not release['assets'], 'Refusing to alter a non-empty draft'
    checksum = root/(NAME+'.sha256')
    checksum.write_text(sha+'  '+NAME+'\n')
    subprocess.run(['gh', 'release', 'upload', TAG, str(apk), str(checksum), '--repo', REPO], check=True)
    release = api('releases/'+str(p['draft_release_id']))
    asset, = [x for x in release['assets'] if x['name'] == NAME]
    assert asset['digest'] == 'sha256:'+sha and asset['size'] == len(data)
    checksum_asset, = [x for x in release['assets'] if x['name'] == checksum.name]
    assert checksum_asset['digest'] == 'sha256:'+hashlib.sha256(checksum.read_bytes()).hexdigest()
    assert len(release['assets']) == 2
    subprocess.run(['gh', 'api', '--method', 'PATCH', f'repos/{REPO}/releases/{p["draft_release_id"]}', '-F', 'draft=false'], check=True)
    public = api('releases/tags/'+TAG)
    assert not public['draft'] and not public['prerelease']
    published_asset, = [x for x in public['assets'] if x['name'] == NAME]
    assert published_asset['digest'] == 'sha256:'+sha and published_asset['id'] == asset['id']
    receipt = dict(source=p['source'], run_id=p['run_id'], artifact_id=p['artifact_id'], artifact_sha256=p['artifact_digest'].removeprefix('sha256:'), apk_sha256=sha, bytes=asset['size'], signer=p['signer'], package='com.universalrp.appdisabler', version='0.5.1', version_code=7, release_id=public['id'], asset_id=asset['id'], published_at=public['published_at'], url=asset['browser_download_url'], rebuilt=False, physical_device_tested=False, baseline='published 0.5.0')
    Path('/tmp/appdisabler051-publication-receipt.json').write_text(json.dumps(receipt, indent=2)+'\n')
    print('::notice title=System App Disabler 0.5.1 published::'+json.dumps(receipt), flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception:
        import traceback
        print('::error title=Disabler publication stopped::'+traceback.format_exc()[-6000:].replace('%', '%25').replace('\n', '%0A'))
        raise
