#!/usr/bin/env python3
"""Raw installed-APK proof INSIDE the disposable emulator session, before teardown."""
import hashlib
import json
import os
from pathlib import Path
import subprocess


def main():
    assert os.environ.get('GITHUB_ACTIONS') == os.environ.get('CI') == 'true'
    def adb(*args):return subprocess.check_output(['adb',*args],text=True).strip()
    assert adb('get-serialno').startswith('emulator-') and adb('shell','getprop','ro.kernel.qemu')=='1'
    p=Path(os.environ['RUNNER_TEMP'])/'health-cleaner/verification.json'
    r=json.loads(p.read_text());assert r['package']=='com.universalrp.cleansweep' and r['version']=='2.97' and r['code']==102
    paths=adb('shell','pm','path',r['package']).splitlines();assert len(paths)==1 and paths[0].startswith('package:/data/app/')
    target=Path('/tmp/liveguard297-installed.apk')
    with target.open('xb') as stream:
        subprocess.run(['adb','exec-out','cat',paths[0].removeprefix('package:')],stdout=stream,check=True)
    digest=hashlib.sha256(target.read_bytes()).hexdigest()
    assert digest==r['sha256'] and target.stat().st_size==r['bytes'],'Final installed APK bytes differ from archived candidate'
    r['final_installed_apk_sha256']=digest
    p.write_text(json.dumps(r,indent=2)+'\n')
    print('::notice title=Exact installed Live Guard2.97::'+json.dumps(dict(sha256=digest,bytes=r['bytes'],raw_transfer=True,scope='Disposable emulator only; not owner phone')),flush=True)

if __name__=='__main__':
    try:main()
    except Exception:
        import traceback
        print('::error title=Installed APK proof::'+traceback.format_exc().replace('%','%25').replace('\n','%0A'),flush=True)
        raise
