#!/usr/bin/env python3
"""Owner-scoped exact-artifact promotion. No rebuild, overwrite or phone-test inheritance."""
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys
import time
import zipfile
from inspect_battery_voice_candidate import APPS, SIGNER
from verify_release_permission_delta import parse_badging

REPO = 'universalrp2003/RP-Android-Apps'
BRANCH = 'arena/e24330d5-rp-android-apps'
def run(*args): return subprocess.check_output(args, text=True).strip()
def api(path): return json.loads(run('gh', 'api', 'repos/'+REPO+'/'+path))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def pages(path):
    result = []
    for page in range(1, 21):
        batch = api(f'{path}?per_page=100&page={page}'); result.extend(batch)
        if len(batch) < 100: return result
    raise ValueError('Pagination limit exceeded; refuse to guess')

def main():
    assert os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ['GITHUB_REF'] == 'refs/heads/'+BRANCH
    assert os.environ['GH_REPO'] == REPO
    pins = json.loads(Path('docs/approved-health-candidates.json').read_text())
    assert pins['publication_authorized'] is True and set(pins['apps']) == {'cleaner', 'voicenotes'}
    source = pins['source']; assert re.fullmatch('[a-f0-9]{40}', source)
    subprocess.run(['git', 'merge-base', '--is-ancestor', source, 'HEAD'], check=True)
    subprocess.run(['git','diff','--exit-code',source,'HEAD','--','cleaner/','voicenotes/','shared/',
                    'gradle/','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat'],check=True)
    workflow = api(f'actions/runs/{pins["run_id"]}')
    assert (workflow['head_sha'],workflow['head_branch'],workflow['status'],workflow['conclusion'],workflow['path']) == (
        source, BRANCH, 'completed','success','.github/workflows/battery-voice-candidates.yml')
    jobs = api(f'actions/runs/{pins["run_id"]}/jobs')['jobs']
    for module in pins['apps']:
        job = next(j for j in jobs if j['name'] == f'Signed {module} update and regression tests')
        assert job['conclusion'] == 'success'
        for name in ['Source, security and bilingual gates','Test, lint and build one signed authorized app',
                     'Actual APK identity, signer continuity and mandatory permission delta',
                     'Exact release update, synthetic data retention and native playback']:
            assert next(s for s in job['steps'] if s['name']==name)['conclusion']=='success'
    old_releases = pages('releases'); tags = pages('tags')
    for module in pins['apps']:
        _, version, _, baseline_tag, _, _, _ = APPS[module]
        tag = ('live-guard' if module == 'cleaner' else 'voice-notes')+'-v'+version
        existing=[r for r in old_releases if r['tag_name']==tag]
        tag_refs=[t for t in tags if t['name']==tag]
        if existing:
            # Only resume the exact draft whose ID/bytes were inspected after our prior
            # publisher stopped. Never recreate it, upload over it or accept arbitrary drafts.
            assert len(existing)==1
            draft=existing[0]
            assert draft['id']==pins['apps'][module].get('resume_draft_release_id')
            assert draft['draft'] and not draft['prerelease'] and draft['target_commitish']==source
            assert not tag_refs or (len(tag_refs)==1 and tag_refs[0]['commit']['sha']==source)
        else:
            assert not pins['apps'][module].get('resume_draft_release_id')
            assert not tag_refs, 'Never replace a release tag'
    sdk = Path(os.environ['ANDROID_HOME'])/'build-tools'
    tools = sorted(sdk.iterdir(), key=lambda p:tuple(int(n) for n in re.findall(r'\d+',p.name)))[-1]
    receipts = []
    for module in ('voicenotes','cleaner'):
        package,version,code,baseline_tag,old_name,old_sha,new_name = APPS[module]
        pin=pins['apps'][module]; artifact=api(f'actions/artifacts/{pin["artifact_id"]}')
        assert not artifact['expired'] and artifact['workflow_run']['id']==pins['run_id']
        assert artifact['workflow_run']['head_sha']==source
        assert artifact['name']==f'verified-{module}-battery-voice-update'
        assert artifact['digest']=='sha256:'+pin['archive_sha256']
        assert artifact['size_in_bytes'] < 80_000_000
        root=Path(os.environ['RUNNER_TEMP'])/('promote-health-'+module); root.mkdir()
        archive=root/'candidate.zip'
        with archive.open('wb') as target:
            subprocess.run(['gh','api',f'repos/{REPO}/actions/artifacts/{pin["artifact_id"]}/zip'],stdout=target,check=True)
        assert sha(archive)==pin['archive_sha256']
        with zipfile.ZipFile(archive) as z:
            assert sum(i.file_size for i in z.infolist()) < 100_000_000
            for item in z.infolist():
                p=Path(item.filename)
                assert not p.is_absolute() and '..' not in p.parts and not stat.S_ISLNK(item.external_attr>>16)
            assert [n for n in z.namelist() if n.endswith('.apk')]==[new_name]
            identity=json.loads(z.read('verification.json'))
            candidate=root/new_name; candidate.write_bytes(z.read(new_name))
        assert (identity['source'],identity['package'],identity['version'],identity['code'],identity['signer']) == (source,package,version,code,SIGNER)
        assert identity['sha256']==pin['apk_sha256']==sha(candidate) and identity['bytes']==pin['bytes']==candidate.stat().st_size
        for gate in ['same_uid','data_preserved','preferences_preserved','restart_passed']: assert identity[gate] is True
        if module=='voicenotes': assert identity['real_hd_playback_passed'] is True
        assert identity['added_permissions']==identity['removed_permissions']==[]
        assert identity['physical_phone_tested'] is False and identity['play_protect_tested'] is False
        assert identity['baseline_tag']==baseline_tag and identity['baseline_sha256']==old_sha
        badging=run(str(tools/'aapt2'),'dump','badging',str(candidate))
        assert f"package: name='{package}' versionCode='{code}' versionName='{version}'" in badging
        assert re.search(r"(?:sdkVersion|minSdkVersion):\s*'26'",badging) and 'application-debuggable' not in badging
        certs=run(str(tools/'apksigner'),'verify','--print-certs',str(candidate))
        assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)',certs))=={SIGNER}
        previous=root/'previous'; previous.mkdir()
        subprocess.run(['gh','release','download',baseline_tag,'--pattern',old_name,'--dir',str(previous)],check=True)
        assert sha(previous/old_name)==old_sha
        previous_badging=run(str(tools/'aapt2'),'dump','badging',str(previous/old_name))
        assert parse_badging(badging)==parse_badging(previous_badging)==set(identity['permissions'])
        new_file=root/'candidate-badging.txt'; old_file=root/'previous-badging.txt'
        new_file.write_text(badging); old_file.write_text(previous_badging)
        delta=run(sys.executable,'tools/verify_release_permission_delta.py','--candidate-badging',str(new_file),'--previous-badging',str(old_file))
        assert 'EVIDENCE-REQUIRED: no' in delta
        checksum=root/(new_name.removesuffix('.apk')+'.sha256'); checksum.write_text(sha(candidate)+'  '+new_name+'\n')
        tag=('live-guard' if module=='cleaner' else 'voice-notes')+'-v'+version
        title=('Live Guard' if module=='cleaner' else 'Voice Notes')+' '+version
        resume_id=pin.get('resume_draft_release_id')
        if not resume_id:
            subprocess.run(['gh','release','create',tag,'--target',source,'--title',title,'--notes-file',f'releases/{tag}.md','--draft',
                            str(candidate)+'#'+new_name,str(checksum)+'#'+checksum.name],check=True)
        for attempt in range(12):
            # GitHub's tag endpoint does not expose a draft before its tag is published.
            drafts=[r for r in pages('releases') if r['tag_name']==tag]
            assert len(drafts)==1
            draft=api('releases/'+str(drafts[0]['id']))
            if resume_id: assert draft['id']==resume_id
            assert draft['target_commitish']==source and draft['draft'] and not draft['prerelease']
            assert len(draft['assets'])==2
            asset=next(a for a in draft['assets'] if a['name']==new_name)
            checksum_asset=next(a for a in draft['assets'] if a['name']==checksum.name)
            if asset.get('digest')=='sha256:'+pin['apk_sha256'] and checksum_asset.get('digest')=='sha256:'+sha(checksum): break
            time.sleep(2)
        else: raise AssertionError('Draft asset digests did not become verifiable; nothing made public')
        assert asset['size']==pin['bytes']
        for phase in ('draft','public'):
            destination=root/phase; destination.mkdir()
            if phase=='draft':
                with (destination/new_name).open('wb') as stream:
                    subprocess.run(['gh','api',f'repos/{REPO}/releases/assets/{asset["id"]}',
                                    '-H','Accept: application/octet-stream'],stdout=stream,check=True)
            else:
                subprocess.run(['gh','release','download',tag,'--pattern',new_name,'--dir',str(destination)],check=True)
            assert sha(destination/new_name)==pin['apk_sha256'] and (destination/new_name).stat().st_size==pin['bytes']
            if phase=='draft': subprocess.run(['gh','release','edit',tag,'--draft=false','--latest=false'],check=True)
        public=api('releases/tags/'+tag); assert not public['draft'] and not public['prerelease']
        asset=next(a for a in public['assets'] if a['name']==new_name)
        assert asset['digest']=='sha256:'+pin['apk_sha256'] and asset['size']==pin['bytes']
        assert asset['browser_download_url']==f'https://github.com/{REPO}/releases/download/{tag}/{new_name}'
        # Immutable baseline IDs, sizes and digests must still match the preflight record.
        before=next(r for r in old_releases if r['tag_name']==baseline_tag)
        after=api('releases/tags/'+baseline_tag)
        assert before['id']==after['id']
        assert [(a['id'],a['name'],a['size'],a.get('digest')) for a in before['assets']] == [(a['id'],a['name'],a['size'],a.get('digest')) for a in after['assets']]
        result=dict(module=module,source=source,run_id=pins['run_id'],artifact_id=pin['artifact_id'],tag=tag,
                    release_id=public['id'],asset_id=asset['id'],published_at=public['published_at'],url=asset['browser_download_url'],
                    bytes=pin['bytes'],sha256=pin['apk_sha256'],signer=SIGNER,physical_phone_tested=False,play_protect_tested=False)
        receipts.append(result)
        print('::notice title=Published exact authorized update::'+json.dumps(result),flush=True)
    Path(os.environ['RUNNER_TEMP'],'health-publication-receipts.json').write_text(json.dumps(receipts,indent=2)+'\n')

if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Exact promotion stopped::'+traceback.format_exc()[-3000:].replace('%','%25').replace('\n','%0A'),flush=True)
        raise
