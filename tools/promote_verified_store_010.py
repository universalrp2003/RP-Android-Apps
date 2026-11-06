#!/usr/bin/env python3
"""Publish only the authorized signed/native-verified Store0.10 artifact, without a rebuild."""
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import time
import zipfile
from promote_verified_health_updates import REPO, api, pages, run, sha
from inspect_battery_voice_candidate import SIGNER
from verify_release_permission_delta import parse_badging

BRANCH='arena/8c5ad59b-rp-android-apps'
TAG='tamilan-store-v0.10'
NAME='Tamilan-Store-v0.10.apk'
OLD_TAG='tamilan-store-v0.9'
OLD_NAME='Tamilan-Store-v0.9.apk'
OLD_SHA='6b19a94be83e47e54093f9f73f9b4798d736857fc4cd16e4e7f10c7424350bab'
PACKAGE='com.universalrp.tamilanstore'

def verify_candidate_gates(pin):
    source = pin['source']
    checks=[(pin['candidate_run'],'.github/workflows/tamilanstore-signed-candidate.yml',
             ['Source and localization gates','Test, lint and build release-key candidate','Pin identity, signer and exact upgrade fixture',
              'Exact signed Store0.9 upgrade and data preservation on Android15','Exact artifact receipt after upgrade testing']),
            (pin['native_run'],'.github/workflows/tamilanstore-preview.yml',
             ['Test and compile preview','Verify real archives and Android installer handoff on Android 15']),
            (pin['background_run'],'.github/workflows/store-background-download.yml',
             ['Compile native screen-off regression and run unit tests','Run reactivecircus/android-emulator-runner@v2'])]
    for run_id,path,required in checks:
        info=api(f'actions/runs/{run_id}')
        assert (info['head_sha'],info['head_branch'],info['status'],info['conclusion'],info['path'])==(source,BRANCH,'completed','success',path)
        jobs=api(f'actions/runs/{run_id}/jobs')['jobs']; assert len(jobs)==1 and jobs[0]['conclusion']=='success'
        for name in required: assert next(s for s in jobs[0]['steps'] if s['name']==name)['conclusion']=='success'

def main():
    assert os.environ.get('GITHUB_ACTIONS')=='true' and os.environ['GITHUB_REF']=='refs/heads/'+BRANCH
    assert os.environ['GH_REPO']==REPO
    assert os.environ['GITHUB_EVENT_NAME']=='push'
    pin=json.loads(Path('docs/approved-store-0.10-candidate.json').read_text())
    assert pin['publication_authorized'] is True
    source=pin['source']; assert re.fullmatch('[a-f0-9]{40}',source)
    subprocess.run(['git','merge-base','--is-ancestor',source,'HEAD'],check=True)
    compiled=['tamilanstore/','shared/','gradle/','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat']
    subprocess.run(['git','diff','--exit-code',source,'HEAD','--',*compiled],check=True)
    verify_candidate_gates(pin)
    artifact=api(f'actions/artifacts/{pin["artifact_id"]}')
    assert not artifact['expired'] and artifact['workflow_run']['id']==pin['candidate_run']
    assert artifact['workflow_run']['head_sha']==source and artifact['digest']=='sha256:'+pin['archive_sha256']
    assert artifact['name']=='Tamilan-Store-0.10-release-key-upgrade-candidate' and artifact['size_in_bytes']==pin['archive_bytes'] and artifact['size_in_bytes']<80_000_000
    root=Path(os.environ['RUNNER_TEMP'])/'publish-store010';root.mkdir()
    archive=root/'candidate.zip'
    with archive.open('wb') as out: subprocess.run(['gh','api',f'repos/{REPO}/actions/artifacts/{pin["artifact_id"]}/zip'],stdout=out,check=True)
    assert sha(archive)==pin['archive_sha256'] and archive.stat().st_size==pin['archive_bytes']
    with zipfile.ZipFile(archive) as z:
        assert len(z.namelist()) == len(set(z.namelist())), 'Duplicate ZIP entry'
        assert set(z.namelist()) == {NAME, NAME.removesuffix('.apk')+'.sha256', 'permissions.json', 'verification.json'}
        assert sum(i.file_size for i in z.infolist())<100_000_000
        for item in z.infolist():
            path=Path(item.filename)
            assert not path.is_absolute() and '..' not in path.parts and not stat.S_ISLNK(item.external_attr>>16)
        assert [n for n in z.namelist() if n.endswith('.apk')]==[NAME]
        receipt=json.loads(z.read('verification.json'))
        candidate=root/NAME;candidate.write_bytes(z.read(NAME))
    assert (receipt['source'],receipt['package'],receipt['from_version'],receipt['to_version'],receipt['version_code'],receipt['signer']) == (source,PACKAGE,'0.9','0.10',12,SIGNER)
    assert receipt['apk_sha256']==pin['apk_sha256']==sha(candidate) and receipt['bytes']==pin['bytes']==candidate.stat().st_size
    for gate in ('debug_over_release_rejected','same_uid','private_file_preserved','language_preserved','motion_preference_preserved','restart_passed'): assert receipt[gate] is True
    for gate in ('physical_phone_tested','play_protect_tested','debug_to_release_upgrade_supported'): assert receipt[gate] is False
    assert receipt['permission_comparison']['added']==receipt['permission_comparison']['removed']==[]
    tools=sorted((Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(),key=lambda p:tuple(int(n) for n in re.findall(r'\d+',p.name)))[-1]
    badging=run(str(tools/'aapt2'),'dump','badging',str(candidate))
    assert f"package: name='{PACKAGE}' versionCode='12' versionName='0.10'" in badging
    assert re.search(r"(?:sdkVersion|minSdkVersion):\s*'26'",badging) and 'application-debuggable' not in badging
    cert=run(str(tools/'apksigner'),'verify','--print-certs',str(candidate))
    assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)',cert))=={SIGNER}
    baseline=root/'baseline';baseline.mkdir()
    subprocess.run(['gh','release','download',OLD_TAG,'--pattern',OLD_NAME,'--dir',str(baseline)],check=True)
    assert sha(baseline/OLD_NAME)==OLD_SHA and (baseline/OLD_NAME).stat().st_size==19309106
    previous=run(str(tools/'aapt2'),'dump','badging',str(baseline/OLD_NAME))
    assert parse_badging(badging)==parse_badging(previous)==set(receipt['permission_comparison']['permissions'])
    (root/'candidate-badging.txt').write_text(badging);(root/'previous-badging.txt').write_text(previous)
    delta=run('python3','tools/verify_release_permission_delta.py','--candidate-badging',str(root/'candidate-badging.txt'),'--previous-badging',str(root/'previous-badging.txt'))
    assert 'EVIDENCE-REQUIRED: no' in delta
    old=api('releases/tags/'+OLD_TAG); assert old['id']==430469827
    releases=pages('releases')
    prior={r['tag_name']:(r['id'],sorted((a['id'],a['name'],a['size'],a.get('digest')) for a in r['assets'])) for r in releases if not r['draft']}
    existing=[r for r in releases if r['tag_name']==TAG]
    refs=[t for t in pages('tags') if t['name']==TAG]
    resume=pin.get('resume_draft_release_id')
    if existing:
        assert len(existing)==1 and existing[0]['id']==resume and existing[0]['draft'] and existing[0]['target_commitish']==source
        assert not refs or (len(refs)==1 and refs[0]['commit']['sha']==source)
    else: assert not resume and not refs
    checksum=root/'Tamilan-Store-v0.10.sha256';checksum.write_text(sha(candidate)+'  '+NAME+'\n')
    if not resume:
        subprocess.run(['gh','release','create',TAG,'--target',source,'--title','Tamilan Store 0.10 — background download recovery','--notes-file','releases/tamilan-store-v0.10.md','--draft',str(candidate)+'#'+NAME,str(checksum)+'#'+checksum.name],check=True)
    for attempt in range(12):
        matches=[r for r in pages('releases') if r['tag_name']==TAG];assert len(matches)==1
        draft=api('releases/'+str(matches[0]['id']))
        assert draft['draft'] and not draft['prerelease'] and draft['target_commitish']==source
        if resume: assert draft['id']==resume
        assert len(draft['assets'])==2
        asset=next(a for a in draft['assets'] if a['name']==NAME)
        checksum_asset=next(a for a in draft['assets'] if a['name']==checksum.name)
        if asset.get('digest')=='sha256:'+pin['apk_sha256'] and checksum_asset.get('digest')=='sha256:'+sha(checksum):break
        time.sleep(2)
    else: raise AssertionError('Draft assets not verified; nothing made public')
    assert asset['size']==pin['bytes']
    draft_file=root/'draft.apk'
    with draft_file.open('wb') as out: subprocess.run(['gh','api',f'repos/{REPO}/releases/assets/{asset["id"]}','-H','Accept: application/octet-stream'],stdout=out,check=True)
    assert sha(draft_file)==pin['apk_sha256'] and draft_file.stat().st_size==pin['bytes']
    subprocess.run(['gh','release','edit',TAG,'--draft=false','--latest=false'],check=True)
    public=api('releases/tags/'+TAG);assert not public['draft'] and not public['prerelease']
    asset=next(a for a in public['assets'] if a['name']==NAME)
    assert asset['size']==pin['bytes'] and asset['digest']=='sha256:'+pin['apk_sha256']
    assert asset['browser_download_url']==f'https://github.com/{REPO}/releases/download/{TAG}/{NAME}'
    published=root/'public';published.mkdir()
    subprocess.run(['gh','release','download',TAG,'--pattern',NAME,'--dir',str(published)],check=True)
    assert sha(published/NAME)==pin['apk_sha256'] and (published/NAME).stat().st_size==pin['bytes']
    after=api('releases/tags/'+OLD_TAG)
    assert old['id']==after['id'] and [(a['id'],a['name'],a['size'],a.get('digest')) for a in old['assets']]==[(a['id'],a['name'],a['size'],a.get('digest')) for a in after['assets']]
    current={r['tag_name']:(r['id'],sorted((a['id'],a['name'],a['size'],a.get('digest')) for a in r['assets'])) for r in pages('releases') if not r['draft']}
    for tag, value in prior.items(): assert current[tag]==value, tag
    result=dict(rebuilt=False,prior_public_assets_unchanged=True,owner_reports_background_download_working=True,installed_phone_hash_independently_verified=False,tag=TAG,source=source,candidate_run=pin['candidate_run'],native_run=pin['native_run'],artifact_id=pin['artifact_id'],release_id=public['id'],asset_id=asset['id'],published_at=public['published_at'],url=asset['browser_download_url'],bytes=pin['bytes'],sha256=pin['apk_sha256'],signer=SIGNER,physical_phone_tested=False,play_protect_tested=False)
    Path(os.environ['RUNNER_TEMP'],'store010-publication-receipt.json').write_text(json.dumps(result,indent=2)+'\n')
    print('::notice title=Published exact Store0.10::'+json.dumps(result),flush=True)

if __name__=='__main__':
    try:main()
    except Exception:
        import traceback
        print('::error title=Store0.10 promotion stopped::'+traceback.format_exc()[-3500:].replace('%','%25').replace('\n','%0A'),flush=True)
        raise
