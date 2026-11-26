#!/usr/bin/env python3
"""Promote the exact owner-authorized, CI-verified 3.02 APK; never rebuild or overwrite."""
import hashlib, json, os, pathlib, re, stat, subprocess, tempfile, zipfile
from verify_release_permission_delta import parse_badging

REPO='universalrp2003/RP-Android-Apps'
RUN=45745935102
SOURCE='621020da7fb70774735aed9e78c1e4108920b9c8'
ARTIFACT=15082234883
ARCHIVE='dd72b21d3c9e3ad602b8e1f5f1636af3152eb568d5e6fcf8964a81eccd0a52a5'
SHA='1422d1efca59f78f2a541d127d406695931b62fc7a65894010be071f2849854f'
SIGNER='7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
TAG='live-guard-v3.02'; NAME='CleanSweep-v3.02.apk'
def cmd(*args): return subprocess.check_output(args,text=True).strip()
def api(path): return json.loads(cmd('gh','api',f'repos/{REPO}/{path}'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    assert os.environ['GITHUB_REF']=='refs/heads/arena/30198928-rp-android-apps'
    run=api(f'actions/runs/{RUN}')
    assert (run['head_sha'],run['head_branch'],run['conclusion'],run['path'])==(SOURCE,'arena/30198928-rp-android-apps','success','.github/workflows/liveguard-302-candidate.yml')
    subprocess.run(['git','merge-base','--is-ancestor',SOURCE,'HEAD'],check=True)
    subprocess.run(['git','diff','--exit-code',SOURCE,'HEAD','--','cleaner/','shared/','gradle/','build.gradle.kts','settings.gradle.kts','gradle.properties','releases/live-guard-v3.02.md'],check=True)
    jobs=api(f'actions/runs/{RUN}/jobs')['jobs']
    for name, step in [('build','Check regression suites, version, signing identity and candidate digest'),('upgrade (published)','Verify exact candidate upgrade and native checks'),('upgrade (owner-candidate)','Verify exact candidate upgrade and native checks')]:
        job,=[j for j in jobs if j['name']==name]; assert job['conclusion']=='success'
        assert next(s for s in job['steps'] if s['name']==step)['conclusion']=='success'
    a=api(f'actions/artifacts/{ARTIFACT}')
    assert a['workflow_run']['id']==RUN and a['workflow_run']['head_sha']==SOURCE and not a['expired']
    assert a['name']=='liveguard-3.02-verified-candidate' and a['digest']=='sha256:'+ARCHIVE
    releases=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100'))
    assert not any(r['tag_name']==TAG for page in releases for r in page), 'Release exists; refusing to overwrite'
    assert subprocess.run(['git','ls-remote','--exit-code','--tags','origin','refs/tags/'+TAG],stdout=subprocess.DEVNULL).returncode != 0, 'Tag exists'
    with tempfile.TemporaryDirectory() as tmp:
        root=pathlib.Path(tmp); archive=root/'artifact.zip'
        with archive.open('wb') as f: subprocess.run(['gh','api',f'repos/{REPO}/actions/artifacts/{ARTIFACT}/zip'],stdout=f,check=True)
        assert sha(archive)==ARCHIVE
        with zipfile.ZipFile(archive) as z:
            assert sum(i.file_size for i in z.infolist()) < 100_000_000
            for i in z.infolist():
                assert not pathlib.Path(i.filename).is_absolute() and '..' not in pathlib.Path(i.filename).parts and not stat.S_ISLNK(i.external_attr >> 16)
            assert z.read('sha256.txt').decode().strip()==SHA
            apk=root/NAME; apk.write_bytes(z.read('candidate.apk'))
        assert sha(apk)==SHA
        tools=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(),key=lambda p:tuple(map(int,re.findall(r'\d+',p.name))))[-1]
        badging=cmd(str(tools/'aapt'),'dump','badging',str(apk)); assert "package: name='com.universalrp.cleansweep' versionCode='107' versionName='3.02'" in badging and 'application-debuggable' not in badging
        cert=cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk)); assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)',cert))=={SIGNER}
        old=root/'baseline';old.mkdir()
        subprocess.run(['gh','release','download','live-guard-v3.01','--repo',REPO,'--pattern','CleanSweep-v3.01.apk','--dir',str(old)],check=True)
        prior=old/'CleanSweep-v3.01.apk'; assert sha(prior)=='b5850343f4788d9c499024da8ea53b534a20e317bf57b071d855e565a360174f'
        before=parse_badging(cmd(str(tools/'aapt'),'dump','badging',str(prior))); after=parse_badging(badging)
        assert not (after-before), f'Unexpected added permissions: {after-before}'
        print('Permission change: added',sorted(after-before),'removed',sorted(before-after),flush=True)
        checksum=root/(NAME+'.sha256'); checksum.write_text(SHA+'  '+NAME+'\n')
        subprocess.run(['gh','release','create',TAG,str(apk),str(checksum),'--repo',REPO,'--target',SOURCE,'--title','Live Guard 3.02 — review fixes','--notes-file','releases/live-guard-v3.02.md','--draft','--latest=false'],check=True)
        releases=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100')); draft,=[r for page in releases for r in page if r['tag_name']==TAG]
        assert draft['draft'] and not draft['prerelease'] and draft['target_commitish']==SOURCE
        asset,=[x for x in draft['assets'] if x['name']==NAME]
        assert asset['digest']=='sha256:'+SHA and asset['size']==apk.stat().st_size
        subprocess.run(['gh','api','--method','PATCH',f'repos/{REPO}/releases/{draft["id"]}','-F','draft=false'],check=True)
        public=api('releases/tags/'+TAG); assert not public['draft'] and not public['prerelease']
        asset,=[x for x in public['assets'] if x['name']==NAME]; assert asset['digest']=='sha256:'+SHA
        receipt=dict(source=SOURCE,run_id=RUN,artifact_id=ARTIFACT,artifact_sha256=ARCHIVE,apk_sha256=SHA,bytes=asset['size'],signer=SIGNER,package='com.universalrp.cleansweep',version='3.02',version_code=107,release_id=public['id'],asset_id=asset['id'],published_at=public['published_at'],url=asset['browser_download_url'],rebuilt=False,physical_phone_tested=False,owner_tested_prior_candidate=True,owner_candidate_installed_hash_verified=False,baselines=['published 3.01','owner 3.01 candidate'])
        print('::notice title=Published exact Live Guard 3.02::'+json.dumps(receipt),flush=True)
        (pathlib.Path(os.environ['RUNNER_TEMP'])/'liveguard302-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Publication stopped::'+traceback.format_exc()[-6000:].replace('%','%25').replace('\n','%0A'))
        raise
