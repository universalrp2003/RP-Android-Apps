#!/usr/bin/env python3
"""Publish only the owner's exact successful Live Guard signed build; never rebuild or replace."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

REPO = 'universalrp2003/RP-Android-Apps'
TAG = 'live-guard-v3.05'
NAME = 'CleanSweep-v3.05.apk'
def cmd(*args): return subprocess.check_output(args, text=True).strip()
def api(path): return json.loads(cmd('gh', 'api', f'repos/{REPO}/{path}'))
def main():
    p = json.loads(Path('docs/approved-live-guard-3.05.json').read_text())
    assert os.environ['GITHUB_REF'] == 'refs/heads/arena/30198928-rp-android-apps' and p['owner_authorized'] is True
    run = api(f'actions/runs/{p["run_id"]}')
    assert (run['head_sha'], run['head_branch'], run['conclusion'], run['run_attempt'], run['path']) == (p['source'], 'arena/30198928-rp-android-apps', 'success', p['run_attempt'], '.github/workflows/liveguard-305-candidate.yml')
    subprocess.run(['git', 'merge-base', '--is-ancestor', p['source'], 'HEAD'], check=True)
    subprocess.run(['git', 'diff', '--exit-code', p['source'], 'HEAD', '--', 'cleaner/', 'shared/', 'gradle/', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties'], check=True)
    jobs = api(f'actions/runs/{p["run_id"]}/jobs')['jobs']
    job, = [j for j in jobs if j['name'] == 'build']
    assert job['conclusion'] == 'success'
    for name in ('Compile signed release APK', 'Verify identity, signer and permissions', 'Archive exact signed candidate'):
        assert next(s for s in job['steps'] if s['name'] == name)['conclusion'] == 'success', name
    a = api(f'actions/artifacts/{p["artifact_id"]}')
    assert a['workflow_run']['id'] == p['run_id'] and a['workflow_run']['head_sha'] == p['source'] and not a['expired']
    assert a['name'] == 'Live-Guard-3.05-signed-candidate' and a['digest'] == p['artifact_digest']
    root = Path('/tmp/liveguard305-publish')
    original = root/'candidate.apk'
    data = original.read_bytes()
    # The pinned GitHub artifact digest identifies the archive; its recorded APK digest is
    # checked against the downloaded bytes before publication. No rebuild is performed.
    p['apk_sha256'] = (root/'sha256.txt').read_text().strip()
    assert re.fullmatch('[a-f0-9]{64}', p['apk_sha256'])
    assert len(data) > 1_000_000 and hashlib.sha256(data).hexdigest() == p['apk_sha256']
    assert (root/'sha256.txt').read_text().strip() == p['apk_sha256']
    apk = root/NAME
    apk.write_bytes(data)
    tools = sorted((Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(), key=lambda d:tuple(map(int,re.findall(r'\d+',d.name))))[-1]
    badge = cmd(str(tools/'aapt'),'dump','badging',str(apk))
    assert "package: name='com.universalrp.cleansweep' versionCode='110' versionName='3.05'" in badge and 'application-debuggable' not in badge
    cert = cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk))
    assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)',cert)) == {p['signer']}
    release = api('releases/'+str(p['draft_release_id']))
    assert release['tag_name'] == TAG and release['target_commitish'] == p['source'] and release['draft'] and not release['prerelease']
    assert release['body'].strip() == Path('releases/live-guard-v3.05.md').read_text().strip()
    assert not release['assets'], 'Refusing to alter a non-empty draft'
    checksum = root/(NAME+'.sha256')
    checksum.write_text(p['apk_sha256']+'  '+NAME+'\n')
    subprocess.run(['gh','release','upload',TAG,str(apk),str(checksum),'--repo',REPO],check=True)
    release = api('releases/'+str(p['draft_release_id']))
    asset, = [x for x in release['assets'] if x['name'] == NAME]
    assert asset['digest'] == 'sha256:'+p['apk_sha256'] and asset['size'] == len(data)
    checksum_asset, = [x for x in release['assets'] if x['name'] == checksum.name]
    assert checksum_asset['digest'] == 'sha256:'+hashlib.sha256(checksum.read_bytes()).hexdigest()
    assert len(release['assets']) == 2
    subprocess.run(['gh','api','--method','PATCH',f'repos/{REPO}/releases/{p["draft_release_id"]}','-F','draft=false'],check=True)
    public = api('releases/tags/'+TAG)
    assert not public['draft'] and not public['prerelease']
    published_asset, = [x for x in public['assets'] if x['name'] == NAME]
    assert published_asset['digest'] == 'sha256:'+p['apk_sha256'] and published_asset['id'] == asset['id']
    receipt = dict(source=p['source'],run_id=p['run_id'],artifact_id=p['artifact_id'],artifact_sha256=p['artifact_digest'].removeprefix('sha256:'),apk_sha256=p['apk_sha256'],bytes=asset['size'],signer=p['signer'],package='com.universalrp.cleansweep',version='3.05',version_code=110,release_id=public['id'],asset_id=asset['id'],published_at=public['published_at'],url=asset['browser_download_url'],rebuilt=False,physical_phone_tested=False,emulator_upgrade_tested=False,baselines=['published 3.04 (permission comparison only)'])
    Path('/tmp/liveguard305-publication-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print('::notice title=Live Guard 3.05 published::'+json.dumps(receipt),flush=True)
if __name__ == '__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Live Guard publication stopped::'+traceback.format_exc()[-6000:].replace('%','%25').replace('\n','%0A'))
        raise
