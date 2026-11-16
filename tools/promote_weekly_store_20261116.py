#!/usr/bin/env python3
"""Owner-authorized exact-byte publication. No build, signing, overwrite or unverified metadata."""
import hashlib, json, os, pathlib, subprocess, tempfile
REPO = 'universalrp2003/RP-Android-Apps'
SIGNER = '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
def cmd(*args):
    return subprocess.check_output(args, text=True).strip()
def api(path):
    return json.loads(cmd('gh', 'api', f'repos/{REPO}/{path}'))
def main():
    approvals = json.loads(pathlib.Path('docs/approved-weekly-store-20261116.json').read_text())
    assert approvals['owner_authorized'] is True
    for p in approvals['publications']:
        assert p['app'] in ('weeklymoney','tamilanstore')
        assert p['signer'] == SIGNER
        run = api(f'actions/runs/{p["run_id"]}')
        assert run['conclusion'] == 'success' and run['head_sha'] == p['source']
        assert run['head_branch'] == 'arena/1cd060b0-rp-android-apps'
        artifacts = api(f'actions/runs/{p["run_id"]}/artifacts')['artifacts']
        a, = [a for a in artifacts if a['name'] == p['artifact'] and not a['expired']]
        assert a['digest'] == 'sha256:' + p['artifact_sha256']
        existing = subprocess.run(['gh','api',f'repos/{REPO}/releases/tags/{p["tag"]}'], capture_output=True, text=True)
        if existing.returncode == 0:
            released = json.loads(existing.stdout)
            assert not released['draft'] and released['prerelease'] == p['prerelease']
            asset, = [a for a in released['assets'] if a['name'] == p['asset']]
            assert asset['digest'] == 'sha256:' + p['sha256'] and asset['size'] == p.get('bytes', asset['size'])
            print('Already published exact bytes:', p['tag']); continue
        assert '404' in existing.stderr, existing.stderr
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            subprocess.run(['gh','run','download',str(p['run_id']),'--repo',REPO,'--name',p['artifact'],'--dir',tmp], check=True)
            files = list(root.rglob(p['input_file']))
            assert len(files) == 1, files
            data = files[0].read_bytes()
            assert hashlib.sha256(data).hexdigest() == p['sha256']
            if 'bytes' in p: assert len(data) == p['bytes']
            p['bytes'] = len(data)
            apk = root / p['asset']; apk.write_bytes(data)
            tools = sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(), key=lambda f: tuple(int(x) for x in f.name.split('.') if x.isdigit()))[-1]
            cert = cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk))
            assert 'certificate SHA-256 digest: '+SIGNER in cert
            badging = cmd(str(tools/'aapt'),'dump','badging',str(apk))
            assert f"package: name='{p['package']}' versionCode='{p['code']}' versionName='{p['version']}'" in badging
            if p['app'] == 'tamilanstore': assert 'application-debuggable' not in badging
            checksum = root/(p['asset']+'.sha256'); checksum.write_text(p['sha256']+'  '+p['asset']+'\n')
            args = ['gh','release','create',p['tag'],str(apk),str(checksum),'--repo',REPO,'--target',p['source'],'--title',p['title'],'--notes-file',p['notes'],'--draft']
            if p['prerelease']: args += ['--prerelease']
            subprocess.run(args, check=True)
            draft = api('releases/tags/'+p['tag'])
            assert draft['draft']
            asset, = [a for a in draft['assets'] if a['name'] == p['asset']]
            assert asset['digest'] == 'sha256:'+p['sha256'] and asset['size'] == p['bytes']
            subprocess.run(['gh','release','edit',p['tag'],'--repo',REPO,'--draft=false'], check=True)
            published = api('releases/tags/'+p['tag'])
            assert not published['draft'] and published['prerelease'] == p['prerelease']
            print('Published exact verified bytes:', p['tag'], p['sha256'])
if __name__ == '__main__':
    try:
        main()
    except Exception:
        import traceback
        message = traceback.format_exc()
        print('::error title=Exact publication verification::' + message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A'))
        raise

