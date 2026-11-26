#!/usr/bin/env python3
"""Publish owner-authorized, CI-tested exact bytes; never rebuild or replace a release."""
import hashlib, json, os, pathlib, subprocess
REPO = 'universalrp2003/RP-Android-Apps'
TAG = 'tamilan-store-v0.18'
NAME = 'Tamilan-Store-v0.18.apk'
def cmd(*args): return subprocess.check_output(args, text=True).strip()
def api(path): return json.loads(cmd('gh', 'api', f'repos/{REPO}/{path}'))
def main():
    p = json.loads(pathlib.Path('docs/approved-tamilan-store-0.18.json').read_text())
    assert p['owner_authorized'] is True
    run = api(f'actions/runs/{p["run_id"]}')
    assert run['conclusion'] == 'success' and run['head_sha'] == p['source'] and run['run_attempt'] == p['run_attempt'] and run['head_branch'] == 'arena/30198928-rp-android-apps'
    artifacts = api(f'actions/runs/{p["run_id"]}/artifacts')['artifacts']
    artifact, = [a for a in artifacts if a['id'] == p['artifact_id'] and a['name'] == 'Tamilan-Store-0.18-release-key-upgrade-candidate' and not a['expired']]
    assert artifact['digest'] == p['artifact_digest']
    root = pathlib.Path('/tmp/tamilan-store-release'); apk = root / NAME
    assert apk.stat().st_size == p['bytes'] and hashlib.sha256(apk.read_bytes()).hexdigest() == p['apk_sha256']
    evidence = json.loads((root/'verification.json').read_text())
    assert evidence['source'] == p['source'] and evidence['apk_sha256'] == p['apk_sha256'] and evidence['bytes'] == p['bytes']
    assert evidence['package'] == 'com.universalrp.tamilanstore' and evidence['from_version'] == '0.17' and evidence['to_version'] == '0.18'
    for key in ('same_uid', 'private_file_preserved', 'language_preserved', 'motion_preference_preserved', 'restart_passed', 'retained_copy_restored_after_upgrade', 'retained_copy_removable', 'manual_catalog_refresh_rendered'): assert evidence[key] is True
    assert evidence['permission_comparison']['added'] == [] and evidence['permission_comparison']['removed'] == []
    assert evidence['signer'] == p['signer'] and evidence['physical_phone_tested'] is False
    tools = sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(), key=lambda f: tuple(int(x) for x in f.name.split('.') if x.isdigit()))[-1]
    assert 'certificate SHA-256 digest: '+p['signer'] in cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk))
    badge = cmd(str(tools/'aapt'),'dump','badging',str(apk))
    assert "package: name='com.universalrp.tamilanstore' versionCode='20' versionName='0.18'" in badge and 'application-debuggable' not in badge
    releases = json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100'))
    matches = [r for page in releases for r in page if r['tag_name'] == TAG]
    # The owner-authorized draft was created separately by the authenticated repository owner.
    # Never create or replace a release or upload onto an unexpected tag.
    assert len(matches) == 1
    release, = matches
    assert not release['prerelease'] and release['target_commitish'] == p['source']
    assert release['draft']
    assert not release['assets'], 'Refusing to replace or append to a nonempty draft'
    upload = subprocess.run(['gh','release','upload',TAG,str(apk),str(root/'Tamilan-Store-v0.18.sha256'),'--repo',REPO], capture_output=True, text=True)
    if upload.returncode:
        raise RuntimeError('Draft asset upload failed: '+(upload.stderr or upload.stdout)[-1500:])
    release = api('releases/'+str(release['id']))
    asset, = [a for a in release['assets'] if a['name'] == NAME]
    assert asset['digest'] == 'sha256:'+p['apk_sha256'] and asset['size'] == p['bytes']
    if release['draft']:
        subprocess.run(['gh','api','--method','PATCH',f'repos/{REPO}/releases/{release["id"]}','-F','draft=false'],check=True)
    published = api('releases/tags/'+TAG)
    assert not published['draft'] and not published['prerelease']
    print('::notice title=Tamilan Store public release::'+json.dumps({'url':published['html_url'],'sha256':p['apk_sha256'],'bytes':p['bytes'],'asset_id':asset['id'],'published_at':published['published_at']}))
if __name__ == '__main__':
    try:
        main()
    except Exception:
        import traceback
        print('::error title=Tamilan Store release verification::'+traceback.format_exc().replace('%','%25').replace('\n','%0A'))
        raise
