#!/usr/bin/env python3
"""Publish only the exact CI-tested, same-signer APKs after the owner's authorization."""
import hashlib, json, os, pathlib, re, subprocess, tempfile, time, zipfile
R='universalrp2003/RP-Android-Apps'
BRANCH='arena/eb6de80e-rp-android-apps'
SIGNER='7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
P=[
 dict(tag='system-app-disabler-v0.4.1',asset='System-App-Disabler-v0.4.1.apk',input='appdisabler-0.4.1.apk',package='com.universalrp.appdisabler',code=5,version='0.4.1',run=44893210469,attempt=2,artifact=14703052949,archive='4d296e2bcf392b4044f7fd12cc340f6055d49f95ec8773e1b901b278c6c1a169',source='558d148f62566fb336d5029356f7efb8fe594e8d',baseline='system-app-disabler-v0.3.0',old='System-App-Disabler-v0.3.0.apk',old_sha='d09ab49e57e7ac3daed7e4edac4a9d8b7fc9292ccbce6ff2a4b48065977f30a0',native=44896432640,native_source='c42e271'),
 dict(tag='elderly-launcher-v1.8',asset='Elderly-Launcher-v1.8.apk',input='Elderly-Launcher-v1.8.apk',package='com.universalrp.launcher',code=9,version='1.8',run=44896014323,attempt=1,artifact=14704067520,archive='c5b68806002acc4bd02f23065d2ce4834d8287c60158a03f3ee6eb72d0e5e65b',source='9bf3c8c636f5d76f963b140c85fc4b214fc2759d',baseline='elderly-launcher-v1.7',old='Elderly-Launcher-v1.7.apk',old_sha='13ce20550e9493f6492a399e5bfad72d8fdd95af9e7d0838b7fd0219f543bfec',native=44896014323,native_source='9bf3c8c'),
]
def cmd(*args): return subprocess.check_output(args,text=True).strip()
def api(path): return json.loads(cmd('gh','api',f'repos/{R}/{path}'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 assert os.environ.get('GITHUB_ACTIONS')=='true' and os.environ.get('GITHUB_REF')=='refs/heads/'+BRANCH
 tools=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(),key=lambda p:tuple(map(int,re.findall(r'\d+',p.name))))[-1]
 from verify_release_permission_delta import parse_badging
 for p in P:
  subprocess.run(['git','merge-base','--is-ancestor',p['source'],'HEAD'],check=True)
  paths=['launcher/src/main','launcher/build.gradle.kts'] if p['package'].endswith('launcher') else ['appdisabler/app/src/main','appdisabler/app/build.gradle.kts']
  subprocess.run(['git','diff','--exit-code',p['source'],'HEAD','--',*paths,'shared','build.gradle.kts','settings.gradle.kts','gradle.properties','gradle'],check=True)
  run=api(f'actions/runs/{p["run"]}'); assert (run['head_sha'],run['head_branch'],run['conclusion'],run['run_attempt'])==(p['source'],BRANCH,'success',p['attempt'])
  native=api(f'actions/runs/{p["native"]}'); assert native['head_sha'].startswith(p['native_source']) and native['head_branch']==BRANCH and native['conclusion']=='success'
  artifact=api(f'actions/artifacts/{p["artifact"]}'); assert artifact['workflow_run']['id']==p['run'] and artifact['workflow_run']['head_sha']==p['source'] and not artifact['expired'] and artifact['digest']=='sha256:'+p['archive']
  pages=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{R}/releases?per_page=100'))
  assert not any(r['tag_name']==p['tag'] for page in pages for r in page), 'Release already exists; stop rather than overwrite'
  assert subprocess.run(['gh','api',f'repos/{R}/git/ref/tags/{p["tag"]}'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode!=0, 'Tag already exists'
  with tempfile.TemporaryDirectory() as tmp:
   d=pathlib.Path(tmp); z=d/'artifact.zip'
   with z.open('wb') as f: subprocess.run(['gh','api',f'repos/{R}/actions/artifacts/{p["artifact"]}/zip'],stdout=f,check=True)
   assert sha(z)==p['archive']
   with zipfile.ZipFile(z) as archive:
    assert archive.namelist()==[p['input']] and archive.getinfo(p['input']).file_size<30_000_000
    apk=d/p['asset']; apk.write_bytes(archive.read(p['input']))
   digest=sha(apk); cert=cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk)); assert 'certificate SHA-256 digest: '+SIGNER in cert
   badging=cmd(str(tools/'aapt'),'dump','badging',str(apk)); assert f"package: name='{p['package']}' versionCode='{p['code']}' versionName='{p['version']}'" in badging and 'application-debuggable' not in badging
   subprocess.run(['gh','release','download',p['baseline'],'--repo',R,'--pattern',p['old'],'--dir',tmp],check=True)
   old=d/p['old']; assert sha(old)==p['old_sha']
   previous=cmd(str(tools/'aapt'),'dump','badging',str(old)); assert parse_badging(previous)==parse_badging(badging), 'Permissions changed'
   checksum=d/(p['asset']+'.sha256'); checksum.write_text(digest+'  '+p['asset']+'\n')
   subprocess.run(['gh','release','create',p['tag'],str(apk),str(checksum),'--repo',R,'--target',p['source'],'--title',p['tag'],'--notes-file','releases/'+p['tag']+'.md','--draft'],check=True)
   for _ in range(15):
    pages=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{R}/releases?per_page=100'))
    matches=[r for page in pages for r in page if r['tag_name']==p['tag']]
    if matches: break
    time.sleep(2)
   assert len(matches)==1 and matches[0]['draft'] and matches[0]['target_commitish']==p['source']
   draft=api('releases/'+str(matches[0]['id'])); asset,=[a for a in draft['assets'] if a['name']==p['asset']]
   assert asset['digest']=='sha256:'+digest and asset['size']==apk.stat().st_size and sorted(a['name'] for a in draft['assets'])==sorted([p['asset'],checksum.name])
   subprocess.run(['gh','api','--method','PATCH',f'repos/{R}/releases/{draft["id"]}','-F','draft=false'],check=True)
   released=api('releases/tags/'+p['tag']); asset,=[a for a in released['assets'] if a['name']==p['asset']]
   assert not released['draft'] and not released['prerelease'] and asset['digest']=='sha256:'+digest
   print('::notice title=Verified release::'+json.dumps(dict(tag=p['tag'],sha256=digest,bytes=asset['size'],published_at=released['published_at'],url=asset['browser_download_url'],build=p['run'],native=p['native'])),flush=True)
if __name__=='__main__':
 try: main()
 except Exception:
  import traceback
  print('::error title=Publication stopped::'+traceback.format_exc().replace('%','%25').replace('\n','%0A'),flush=True); raise
