#!/usr/bin/env python3
"""Publish owner-authorized exact tested Live Guard 3.01, never rebuild or overwrite."""
import hashlib, json, os, pathlib, re, stat, subprocess, tempfile, zipfile
from verify_release_permission_delta import parse_badging

REPO='universalrp2003/RP-Android-Apps'
RUN=43881834720
SOURCE='e8f763f33f0c29ae6d771a3235e1a19096143434'
ARTIFACT=14231804036
ARCHIVE='41831d92334ff8110340c354bd85382256a070bf0957a71bb8880a13659cbce1'
SIGNER='7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
TAG='live-guard-v3.01'; NAME='CleanSweep-v3.01.apk'
def cmd(*args): return subprocess.check_output(args,text=True).strip()
def api(path): return json.loads(cmd('gh','api',f'repos/{REPO}/{path}'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    assert os.environ['GITHUB_REF']=='refs/heads/arena/dc49fff8-rp-android-apps'
    run=api(f'actions/runs/{RUN}')
    assert (run['head_sha'],run['head_branch'],run['conclusion'],run['path'])==(SOURCE,'arena/dc49fff8-rp-android-apps','success','.github/workflows/liveguard-radio-timeout.yml')
    subprocess.run(['git','merge-base','--is-ancestor',SOURCE,'HEAD'],check=True)
    subprocess.run(['git','diff','--exit-code',SOURCE,'HEAD','--','cleaner/','shared/','gradle/','build.gradle.kts','settings.gradle.kts','gradle.properties'],check=True)
    jobs=api(f'actions/runs/{RUN}/jobs')['jobs']; job,=[j for j in jobs if j['name']=='verify']; assert job['conclusion']=='success'
    for name in ('Source contract, JVM tests, lint and signed candidate','Android 16 in-place upgrade, accelerated timeout and shared notification','Verify native tests ran without failures'):
        assert next(s for s in job['steps'] if s['name']==name)['conclusion']=='success'
    a=api(f'actions/artifacts/{ARTIFACT}')
    assert a['workflow_run']['id']==RUN and a['workflow_run']['head_sha']==SOURCE and not a['expired']
    assert a['name']=='Live-Guard-3.01-radio-timeout-candidate' and a['digest']=='sha256:'+ARCHIVE
    releases=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100'))
    assert not any(r['tag_name']==TAG for page in releases for r in page), 'Release already exists; do not overwrite'
    assert subprocess.run(['git','ls-remote','--exit-code','--tags','origin','refs/tags/'+TAG],stdout=subprocess.DEVNULL).returncode != 0, 'Tag already exists'
    with tempfile.TemporaryDirectory() as tmp:
        root=pathlib.Path(tmp); archive=root/'artifact.zip'
        with archive.open('wb') as f: subprocess.run(['gh','api',f'repos/{REPO}/actions/artifacts/{ARTIFACT}/zip'],stdout=f,check=True)
        assert sha(archive)==ARCHIVE
        with zipfile.ZipFile(archive) as z:
            assert sum(i.file_size for i in z.infolist()) < 100_000_000
            for i in z.infolist():
                assert not pathlib.Path(i.filename).is_absolute() and '..' not in pathlib.Path(i.filename).parts and not stat.S_ISLNK(i.external_attr >> 16)
            names=[n for n in z.namelist() if n.endswith('Live-Guard-3.01-candidate.apk')]; assert len(names)==1
            sums=[n for n in z.namelist() if n.endswith('sha256.txt')]; assert len(sums)==1
            expected=z.read(sums[0]).decode().split()[0]; assert re.fullmatch('[a-f0-9]{64}',expected)
            apk=root/NAME; apk.write_bytes(z.read(names[0]))
        assert sha(apk)==expected
        tools=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(),key=lambda p:tuple(map(int,re.findall(r'\d+',p.name))))[-1]
        badging=cmd(str(tools/'aapt'),'dump','badging',str(apk)); assert "package: name='com.universalrp.cleansweep' versionCode='106' versionName='3.01'" in badging and 'application-debuggable' not in badging
        cert=cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk)); assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)',cert))=={SIGNER}
        old=root/'baseline';old.mkdir()
        subprocess.run(['gh','release','download','live-guard-v3.00','--repo',REPO,'--pattern','CleanSweep-v3.00.apk','--dir',str(old)],check=True)
        prior=old/'CleanSweep-v3.00.apk'; assert sha(prior)=='286f172054f0a9b10a16d673f49acff1a3bac3d8b3686c9ddf65c27b2b2a5940'
        oldbadging=cmd(str(tools/'aapt'),'dump','badging',str(prior)); before=parse_badging(oldbadging); after=parse_badging(badging)
        assert not (after-before), f'Unexpected added permissions: {after-before}'
        assert (before-after) <= {'android.permission.FOREGROUND_SERVICE_DATA_SYNC'}, f'Unexpected removed permissions: {before-after}'
        print('Permission change:', 'added',sorted(after-before),'removed',sorted(before-after))
        checksum=root/(NAME+'.sha256'); checksum.write_text(expected+'  '+NAME+'\n')
        subprocess.run(['gh','release','create',TAG,str(apk),str(checksum),'--repo',REPO,'--target',SOURCE,'--title','Live Guard 3.01 — radio timeout crash fix','--notes-file','releases/live-guard-v3.01.md','--draft','--latest=false'],check=True)
        releases=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100')); draft,=[r for page in releases for r in page if r['tag_name']==TAG]
        assert draft['draft'] and not draft['prerelease'] and draft['target_commitish']==SOURCE
        asset,=[x for x in draft['assets'] if x['name']==NAME]
        assert asset['digest']=='sha256:'+expected and asset['size']==apk.stat().st_size
        subprocess.run(['gh','api','--method','PATCH',f'repos/{REPO}/releases/{draft["id"]}','-F','draft=false'],check=True)
        public=api('releases/tags/'+TAG); assert not public['draft'] and not public['prerelease']
        asset,=[x for x in public['assets'] if x['name']==NAME]; assert asset['digest']=='sha256:'+expected
        receipt=dict(source=SOURCE,run_id=RUN,artifact_id=ARTIFACT,artifact_sha256=ARCHIVE,apk_sha256=expected,bytes=asset['size'],signer=SIGNER,package='com.universalrp.cleansweep',version='3.01',version_code=106,release_id=public['id'],asset_id=asset['id'],published_at=public['published_at'],url=asset['browser_download_url'],rebuilt=False,physical_phone_tested=False)
        print('::notice title=Published exact Live Guard 3.01::'+json.dumps(receipt),flush=True)
        (pathlib.Path(os.environ['RUNNER_TEMP'])/'liveguard301-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Publication stopped::'+traceback.format_exc()[-6000:].replace('%','%25').replace('\n','%0A'))
        raise
