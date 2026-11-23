#!/usr/bin/env python3
"""Owner-authorized exact-byte publication of Tamilan Store 0.16. No build, signing, overwrite or unverified metadata."""
import hashlib, json, os, pathlib, subprocess, tempfile
REPO = 'universalrp2003/RP-Android-Apps'
SIGNER = '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
def cmd(*args):
    return subprocess.check_output(args, text=True).strip()
def api(path):
    return json.loads(cmd('gh', 'api', f'repos/{REPO}/{path}'))
def all_releases():
    pages = json.loads(cmd('gh', 'api', '--paginate', '--slurp', f'repos/{REPO}/releases?per_page=100'))
    return [r for page in pages for r in page]
def main():
    approvals = json.loads(pathlib.Path('docs/approved-tamilan-store-0.16.json').read_text())
    assert approvals['owner_authorized'] is True
    for p in approvals['publications']:
        assert p['app'] == 'tamilanstore' and p['tag'] == 'tamilan-store-v0.16'
        assert p['signer'] == SIGNER
        assert p['package'] == 'com.universalrp.tamilanstore' and p['code'] == 18 and p['version'] == '0.16'
        run = api(f'actions/runs/{p["run_id"]}')
        assert run['conclusion'] == 'success' and run['head_sha'] == p['source'] and run['run_attempt'] == p['run_attempt']
        assert run['head_branch'] == p.get('source_branch', 'arena/c1856bf9-rp-android-apps')
        artifacts = api(f'actions/runs/{p["run_id"]}/artifacts')['artifacts']
        a, = [a for a in artifacts if a['name'] == p['artifact'] and not a['expired']]
        assert a['id'] == p['artifact_id'] and a['digest'] == 'sha256:' + p['artifact_sha256']
        # GitHub's by-tag endpoint does not return drafts. Enumerate before creating,
        # or a retry can create another unpublished draft for the same tag.
        matches = [r for r in all_releases() if r['tag_name'] == p['tag']]
        if matches:
            released = sorted(matches, key=lambda r: (r['draft'], r['id']))[0]
            assert released['prerelease'] == p['prerelease']
            assert released['target_commitish'] == p['source']
            asset, = [a for a in released['assets'] if a['name'] == p['asset']]
            assert asset['digest'] == 'sha256:' + p['sha256'] and asset['size'] == p.get('bytes', asset['size'])
            if released['draft']:
                subprocess.run(['gh', 'api', '--method', 'PATCH', f'repos/{REPO}/releases/{released["id"]}', '-F', 'draft=false'], check=True)
            print('Published exact bytes already verified:', p['tag'], json.dumps({'release_id': released['id'], 'asset_id': asset['id'], 'published_at': released['published_at']}))
            continue
        # The public 0.15 release must be untouched: the candidate is an in-place upgrade over it.
        previous, = [r for r in all_releases() if r['tag_name'] == 'tamilan-store-v0.15' and not r['draft']]
        previous_asset, = [a for a in previous['assets'] if a['name'] == 'Tamilan-Store-v0.15.apk']
        assert previous_asset['digest'] == 'sha256:c9284fbf067c664bf496c39bc0dee9230b8e1510a4882757d933013d9bcfb84b' and previous_asset['size'] == 20713379
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            subprocess.run(['gh','run','download',str(p['run_id']),'--repo',REPO,'--name',p['artifact'],'--dir',tmp], check=True)
            files = list(root.rglob(p['input_file']))
            assert len(files) == 1, files
            data = files[0].read_bytes()
            assert hashlib.sha256(data).hexdigest() == p['sha256']
            if 'bytes' in p: assert len(data) == p['bytes']
            p['bytes'] = len(data)
            # The emulator upgrade evidence in the same artifact must describe these exact bytes.
            evidence, = list(root.rglob('verification.json'))
            verification = json.loads(evidence.read_text())
            assert verification['apk_sha256'] == p['sha256'] and verification['bytes'] == p['bytes'] and verification['source'] == p['source']
            assert verification['signer'] == SIGNER and verification['package'] == p['package']
            assert verification['from_version'] == '0.15' and verification['to_version'] == '0.16' and verification['version_code'] == 18
            for flag in ('same_uid', 'private_file_preserved', 'language_preserved', 'motion_preference_preserved', 'restart_passed', 'debug_over_release_rejected',
                         'retained_copy_reproduced_on_0_15', 'retained_copy_removable', 'refresh_enabled_after_removal'):
                assert verification[flag] is True, flag
            apk = root / p['asset']; apk.write_bytes(data)
            tools = sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(), key=lambda f: tuple(int(x) for x in f.name.split('.') if x.isdigit()))[-1]
            cert = cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk))
            assert 'certificate SHA-256 digest: '+SIGNER in cert
            badging = cmd(str(tools/'aapt'),'dump','badging',str(apk))
            assert f"package: name='{p['package']}' versionCode='{p['code']}' versionName='{p['version']}'" in badging
            assert 'application-debuggable' not in badging
            checksum = root/(p['asset']+'.sha256'); checksum.write_text(p['sha256']+'  '+p['asset']+'\n')
            args = ['gh','release','create',p['tag'],str(apk),str(checksum),'--repo',REPO,'--target',p['source'],'--title',p['title'],'--notes-file',p['notes'],'--draft']
            if p['prerelease']: args += ['--prerelease']
            subprocess.run(args, check=True)
            draft, = [r for r in all_releases() if r['tag_name'] == p['tag'] and r['draft']]
            assert draft['draft']
            asset, = [a for a in draft['assets'] if a['name'] == p['asset']]
            assert asset['digest'] == 'sha256:'+p['sha256'] and asset['size'] == p['bytes']
            subprocess.run(['gh','api','--method','PATCH',f'repos/{REPO}/releases/{draft["id"]}','-F','draft=false'], check=True)
            published = api('releases/tags/'+p['tag'])
            assert not published['draft'] and published['prerelease'] == p['prerelease']
            still, = [r for r in all_releases() if r['tag_name'] == 'tamilan-store-v0.15']
            still_asset, = [a for a in still['assets'] if a['name'] == 'Tamilan-Store-v0.15.apk']
            assert still_asset['digest'] == previous_asset['digest'] and still_asset['size'] == previous_asset['size']
            print('Published exact verified bytes:', p['tag'], p['sha256'], p['bytes'], 'bytes')
            print('RECEIPT ' + json.dumps({'release_id': published['id'], 'asset_id': asset['id'], 'published_at': published['published_at'], 'bytes': p['bytes'], 'verification': verification}))
if __name__ == '__main__':
    try:
        main()
    except Exception:
        import traceback
        message = traceback.format_exc()
        print('::error title=Exact publication verification::' + message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A'))
        raise
