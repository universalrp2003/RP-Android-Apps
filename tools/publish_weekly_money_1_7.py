#!/usr/bin/env python3
"""Publish the owner-authorized exact Weekly Money 1.7 candidate; never rebuild or overwrite."""
import hashlib, json, os, pathlib, re, stat, subprocess, tempfile, zipfile
from verify_release_permission_delta import parse_badging

REPO='universalrp2003/RP-Android-Apps'
BRANCH='arena/30198928-rp-android-apps'
RUN=45768339537
SOURCE='a56d5cce920838856df66143c85a417452a33ab4'
ARTIFACT=15092504540
ARCHIVE='34e79d0974b0186d09831ea1c2d06b0016b5fa68d9745d435d1af07767231abe'
SHA='0dac276f391fb4099fe13ac45d67adfade6fe245b644d634a87a6b6af1cbc81a'
SIGNER='7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
TAG='weekly-money-v1.7'; NAME='Weekly-Money-v1.7.apk'
def cmd(*args): return subprocess.check_output(args,text=True).strip()
def api(path): return json.loads(cmd('gh','api',f'repos/{REPO}/{path}'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    assert os.environ['GITHUB_REF']=='refs/heads/'+BRANCH
    run=api(f'actions/runs/{RUN}')
    assert (run['head_sha'],run['head_branch'],run['conclusion'],run['path'])==(SOURCE,BRANCH,'success','.github/workflows/weeklymoney-page-fix.yml')
    subprocess.run(['git','merge-base','--is-ancestor',SOURCE,'HEAD'],check=True)
    subprocess.run(['git','diff','--exit-code',SOURCE,'HEAD','--','weeklymoney/app/','weeklymoney/core/','gradle/','build.gradle.kts','settings.gradle.kts','gradle.properties'],check=True)
    jobs=api(f'actions/runs/{RUN}/jobs')['jobs']; job,=[j for j in jobs if j['name']=='verify']; assert job['conclusion']=='success'
    for name in ('Source checks and signed build','Check release package, version, signer and digest','Exercise page retention and encrypted in-place upgrade','Verify all targeted native tests ran'):
        assert next(s for s in job['steps'] if s['name']==name)['conclusion']=='success'
    artifact=api(f'actions/artifacts/{ARTIFACT}')
    assert artifact['workflow_run']['id']==RUN and artifact['workflow_run']['head_sha']==SOURCE and not artifact['expired']
    assert artifact['name']=='WeeklyMoney-1.7-summary-fix-candidate' and artifact['digest']=='sha256:'+ARCHIVE
    releases=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100'))
    assert not any(r['tag_name']==TAG for page in releases for r in page), 'Release exists: refusing to overwrite'
    assert subprocess.run(['git','ls-remote','--exit-code','--tags','origin','refs/tags/'+TAG],stdout=subprocess.DEVNULL).returncode != 0, 'Tag exists'
    with tempfile.TemporaryDirectory() as temp:
        root=pathlib.Path(temp); archive=root/'artifact.zip'
        with archive.open('wb') as f: subprocess.run(['gh','api',f'repos/{REPO}/actions/artifacts/{ARTIFACT}/zip'],stdout=f,check=True)
        assert sha(archive)==ARCHIVE
        with zipfile.ZipFile(archive) as z:
            assert sum(i.file_size for i in z.infolist()) < 50_000_000
            for i in z.infolist():
                assert not pathlib.Path(i.filename).is_absolute() and '..' not in pathlib.Path(i.filename).parts and not stat.S_ISLNK(i.external_attr >> 16)
            assert z.read(NAME+'.sha256').decode().split()[0]==SHA
            apk=root/NAME; apk.write_bytes(z.read(NAME))
            for label in ('public15','candidate16'):
                proof=json.loads(z.read('release-upgrade-receipt-'+label+'.json'))
                assert proof['apk_sha256']==SHA and proof['apk_bytes']==7499595 and proof['candidate']=='candidate1.7/code19'
                assert proof['same_uid'] and proof['encrypted_notebook_byte_identical'] and proof['master_unlock_existing_volume']
        assert sha(apk)==SHA and apk.stat().st_size==7499595
        tools=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(),key=lambda p:tuple(map(int,re.findall(r'\d+',p.name))))[-1]
        badging=cmd(str(tools/'aapt'),'dump','badging',str(apk))
        assert "package: name='com.universalrp.weeklymoney.preview.notebook' versionCode='19' versionName='1.7'" in badging and 'application-debuggable' not in badging
        assert "application-label:'வார வசூல்'" in badging
        cert=cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk)); assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)',cert))=={SIGNER}
        olddir=root/'baseline';olddir.mkdir()
        subprocess.run(['gh','release','download','weekly-money-v1.5','--repo',REPO,'--pattern','Weekly-Money-v1.5.apk','--dir',str(olddir)],check=True)
        prior=olddir/'Weekly-Money-v1.5.apk'; assert sha(prior)=='b2169439987275201c2a1b85d06f57f39e8791d87a0c782bbe5694d4744a56f6'
        before=parse_badging(cmd(str(tools/'aapt'),'dump','badging',str(prior))); after=parse_badging(badging)
        assert not (after-before), f'Unexpected added permissions: {after-before}'
        checksum=root/(NAME+'.sha256'); checksum.write_text(SHA+'  '+NAME+'\n')
        subprocess.run(['gh','release','create',TAG,str(apk),str(checksum),'--repo',REPO,'--target',SOURCE,'--title','வார வசூல் · Weekly Money 1.7','--notes-file','releases/weekly-money-v1.7.md','--draft','--latest=false'],check=True)
        releases=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{REPO}/releases?per_page=100')); draft,=[r for page in releases for r in page if r['tag_name']==TAG]
        assert draft['draft'] and not draft['prerelease'] and draft['target_commitish']==SOURCE
        asset,=[a for a in draft['assets'] if a['name']==NAME]; assert asset['digest']=='sha256:'+SHA and asset['size']==apk.stat().st_size
        subprocess.run(['gh','api','--method','PATCH',f'repos/{REPO}/releases/{draft["id"]}','-F','draft=false'],check=True)
        published=api('releases/tags/'+TAG); assert not published['draft'] and not published['prerelease']
        asset,=[a for a in published['assets'] if a['name']==NAME]; assert asset['digest']=='sha256:'+SHA
        receipt=dict(app='weeklymoney',source=SOURCE,source_branch=BRANCH,run_id=RUN,artifact_id=ARTIFACT,artifact_sha256=ARCHIVE,apk_sha256=SHA,sha256=SHA,bytes=asset['size'],signer=SIGNER,package='com.universalrp.weeklymoney.preview.notebook',version='1.7',version_code=19,release_id=published['id'],asset_id=asset['id'],published_at=published['published_at'],url=asset['browser_download_url'],publisher_run=int(os.environ['GITHUB_RUN_ID']),rebuilt=False,owner_reported_phone_test=True,phone_apk_hash_verified=False,emulator_upgrade_verified=True)
        print('::notice title=Published exact Weekly Money 1.7::'+json.dumps(receipt),flush=True)
        (pathlib.Path(os.environ['RUNNER_TEMP'])/'weeklymoney17-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Publication stopped::'+traceback.format_exc()[-6000:].replace('%','%25').replace('\n','%0A'))
        raise
