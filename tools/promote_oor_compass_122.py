#!/usr/bin/env python3
"""Promote the owner-accepted CI artifact, never rebuild or replace a public release."""
import hashlib, json, os, pathlib, re, subprocess, tempfile, time, zipfile
R='universalrp2003/RP-Android-Apps'; BRANCH='arena/440b6275-rp-android-apps'
SRC='e592d1b0aefcd3a7e8e9806a0ba09cf12fde8436'; RUN=44217614992; ART=14388384696
APK='Oor-Arivippu-v1.1.22.apk'; TAG='oor-arivippu-v1.1.22'
SHA='5d8777f1f9eb6ed8eeb87fbcf89cd1bffc30d5f7f78cca43e8f93ff7e848bd1d'
ARCHIVE='25407b941b5062c609b47fbfeecf6148b9c27abacb384ff187ad04ca8cf12a29'
SIGNER='7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
def cmd(*a): return subprocess.check_output(a,text=True).strip()
def api(path): return json.loads(cmd('gh','api',f'repos/{R}/{path}'))
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert os.environ.get('GITHUB_ACTIONS')=='true' and os.environ['GITHUB_REF']=='refs/heads/'+BRANCH
 approval=json.loads(pathlib.Path('docs/oor-compass-1.1.22-publication-approved.json').read_text()); assert approval == {'owner_authorized': True, 'tag': TAG, 'apk_sha256': SHA, 'build_run': RUN, 'source': SRC}
 subprocess.run(['git','merge-base','--is-ancestor',SRC,'HEAD'],check=True)
 subprocess.run(['git','diff','--exit-code',SRC,'HEAD','--','oorarivippu/','shared/','build.gradle.kts','settings.gradle.kts','gradle/','gradle.properties'],check=True)
 run=api(f'actions/runs/{RUN}'); assert (run['head_sha'],run['head_branch'],run['conclusion'])==(SRC,BRANCH,'success')
 job=api('actions/jobs/134511268051'); assert job['run_id']==RUN and job['conclusion']=='success'
 native=api('actions/runs/44221838446'); assert native['head_branch']==BRANCH and native['conclusion']=='success'
 artifact=api(f'actions/artifacts/{ART}'); assert artifact['workflow_run']['id']==RUN and artifact['workflow_run']['head_sha']==SRC and not artifact['expired'] and artifact['digest']=='sha256:'+ARCHIVE
 assert api('releases/tags/oor-arivippu-v1.1.21')['id']==439077106
 existing=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{R}/releases?per_page=100'))
 assert not any(x['tag_name']==TAG for page in existing for x in page), 'Release exists; refuse to overwrite or resume'
 p=subprocess.run(['gh','api',f'repos/{R}/git/ref/tags/{TAG}'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL); assert p.returncode!=0, 'Tag already exists'
 with tempfile.TemporaryDirectory() as t:
  root=pathlib.Path(t); archive=root/'candidate.zip'
  with archive.open('wb') as f: subprocess.run(['gh','api',f'repos/{R}/actions/artifacts/{ART}/zip'],stdout=f,check=True)
  assert digest(archive)==ARCHIVE
  with zipfile.ZipFile(archive) as z:
   assert z.namelist()==[APK] and z.getinfo(APK).file_size<30_000_000
   apk=root/APK; apk.write_bytes(z.read(APK))
  assert digest(apk)==SHA
  tools=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(),key=lambda p:tuple(map(int,re.findall(r'\d+',p.name))))[-1]
  signer=cmd(str(tools/'apksigner'),'verify','--print-certs',str(apk)); assert 'certificate SHA-256 digest: '+SIGNER in signer
  badging=cmd(str(tools/'aapt'),'dump','badging',str(apk)); assert "package: name='com.oorarivippu.voiceannouncer' versionCode='27' versionName='1.1.22'" in badging and 'application-debuggable' not in badging
  subprocess.run(['gh','release','download','oor-arivippu-v1.1.21','--repo',R,'--pattern','Oor-Arivippu-v1.1.21.apk','--dir',t],check=True)
  base=root/'Oor-Arivippu-v1.1.21.apk'; assert digest(base)=='e65e1bdbabebc89edbe994894eb1554ce1c7583b8cdb8940aaf0eee054975258'
  previous=cmd(str(tools/'aapt'),'dump','badging',str(base))
  from verify_release_permission_delta import parse_badging
  assert parse_badging(previous)==parse_badging(badging), 'Permission delta requires review'
  sidecar=root/(APK+'.sha256'); sidecar.write_text(SHA+'  '+APK+'\n')
  subprocess.run(['gh','release','create',TAG,str(apk),str(sidecar),'--repo',R,'--target',SRC,'--title','Oor Arivippu 1.1.22 — compass and destination direction','--notes-file','releases/'+TAG+'.md','--draft'],check=True)
  for _ in range(15):
   releases=json.loads(cmd('gh','api','--paginate','--slurp',f'repos/{R}/releases?per_page=100'))
   matches=[x for page in releases for x in page if x['tag_name']==TAG]
   if matches: break
   time.sleep(2)
  assert len(matches)==1 and matches[0]['draft'] and matches[0]['target_commitish']==SRC
  draft=api('releases/'+str(matches[0]['id'])); assets=draft['assets']
  asset,=[a for a in assets if a['name']==APK]
  assert asset['digest']=='sha256:'+SHA and asset['size']==apk.stat().st_size
  assert sorted(a['name'] for a in assets)==sorted([APK,sidecar.name])
  subprocess.run(['gh','api','--method','PATCH',f'repos/{R}/releases/{draft["id"]}','-F','draft=false'],check=True)
  public=api('releases/tags/'+TAG); assert not public['draft'] and not public['prerelease']
  asset,=[a for a in public['assets'] if a['name']==APK]; assert asset['digest']=='sha256:'+SHA
  receipt={'tag':TAG,'release_id':public['id'],'asset_id':asset['id'],'bytes':asset['size'],'published_at':public['published_at'],'url':asset['browser_download_url'],'apk_sha256':SHA,'signer':SIGNER,'build_run':RUN,'about_native_run':44221838446,'source':SRC,'owner_reports_working_on_phone':True,'installed_phone_hash_independently_verified':False,'new_permissions':False}
  pathlib.Path(os.environ['RUNNER_TEMP'],'oor122-publication.json').write_text(json.dumps(receipt,indent=2)+'\n')
  print('::notice title=Published exact accepted APK::'+json.dumps(receipt),flush=True)
if __name__=='__main__':
 try: main()
 except Exception:
  import traceback
  print('::error title=Oor publication held::'+traceback.format_exc().replace('\n','%0A'),flush=True)
  raise
