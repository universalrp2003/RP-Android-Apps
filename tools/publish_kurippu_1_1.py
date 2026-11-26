#!/usr/bin/env python3
"""Publish owner-authorized, CI-tested exact bytes; never rebuild or replace a release."""
import hashlib, json, os, pathlib, subprocess
REPO = 'universalrp2003/RP-Android-Apps'
TAG = 'kurippu-v1.1'
NAME = 'Kurippu-v1.1.apk'
def cmd(*args): return subprocess.check_output(args, text=True).strip()
def api(path): return json.loads(cmd('gh', 'api', f'repos/{REPO}/{path}'))
def main():
    p = json.loads(pathlib.Path('docs/kurippu/approved-1.1.json').read_text())
    assert p['owner_authorized'] is True
    run = api(f'actions/runs/{p["run_id"]}')
    assert run['conclusion'] == 'success' and run['head_sha'] == p['source'] and run['run_attempt'] == p['run_attempt'] and run['head_branch'] == 'arena/30198928-rp-android-apps'
    artifacts = api(f'actions/runs/{p["run_id"]}/artifacts')['artifacts']
    artifact, = [a for a in artifacts if a['id'] == p['artifact_id'] and a['name'] == 'Kurippu-1.1-candidate' and not a['expired']]
    assert artifact['digest'] == p['artifact_digest']
    root = pathlib.Path('/tmp/kurippu-release'); apk = root / NAME
    assert apk.stat().st_size == p['bytes'] and hashlib.sha256(apk.read_bytes()).hexdigest() == p['apk_sha256']
    evidence = json.loads((root/'verification.json').read_text())
    assert evidence['source'] == p['source'] and evidence['apk_sha256'] == p['apk_sha256'] and evidence['bytes'] == p['bytes']
    assert evidence['package'] == 'com.universalrp.kurippu' and evidence['from_version'] == '1.0' and evidence['to_version'] == '1.1'
    for key in ('same_uid', 'expense_entries_and_total_preserved', 'note_preserved', 'about_version_and_warning'): assert evidence[key] is True
    tools = sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(), key=lambda f: tuple(int(x) for x in f.name.split('.') if x.isdigit()))[-1]
    assert 'certificate SHA-256 digest: '+p['signer'] in cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk))
    badge = cmd(str(tools/'aapt'),'dump','badging',str(apk))
    assert "package: name='com.universalrp.kurippu' versionCode='8' versionName='1.1'" in badge and 'application-debuggable' not in badge
    releases = json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100'))
    matches = [r for page in releases for r in page if r['tag_name'] == TAG]
    if not matches:
        created = subprocess.run(['gh','release','create',TAG,str(apk),str(root/(NAME+'.sha256')),'--repo',REPO,'--target',p['source'],'--title','குறிப்பு · Note 1.1','--notes-file','docs/kurippu/release-1.1-notes.md','--draft'], capture_output=True, text=True)
        if created.returncode:
            raise RuntimeError('GitHub release creation failed: ' + (created.stderr or created.stdout)[-1500:])
        releases = json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100'))
        matches = [r for page in releases for r in page if r['tag_name'] == TAG]
    release, = matches
    assert not release['prerelease'] and release['target_commitish'] == p['source']
    asset, = [a for a in release['assets'] if a['name'] == NAME]
    assert asset['digest'] == 'sha256:'+p['apk_sha256'] and asset['size'] == p['bytes']
    if release['draft']:
        subprocess.run(['gh','api','--method','PATCH',f'repos/{REPO}/releases/{release["id"]}','-F','draft=false'],check=True)
    published = api('releases/tags/'+TAG)
    assert not published['draft'] and not published['prerelease']
    print('::notice title=Kurippu public release::'+json.dumps({'url':published['html_url'],'sha256':p['apk_sha256'],'bytes':p['bytes'],'asset_id':asset['id'],'published_at':published['published_at']}))
if __name__ == '__main__':
    try:
        main()
    except Exception:
        import traceback
        print('::error title=Kurippu exact release verification::' + traceback.format_exc().replace('%', '%25').replace('\n', '%0A'))
        raise
