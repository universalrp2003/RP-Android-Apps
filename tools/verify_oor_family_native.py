#!/usr/bin/env python3
"""Require real native results and exact installed release APK BEFORE CI emulator shuts down."""
import hashlib,json,os,subprocess,runpy
from pathlib import Path
import xml.etree.ElementTree as ET
def main():
    native=[c for p in Path('oorarivippu/build/outputs/androidTest-results/connected').rglob('*.xml') for c in ET.parse(p).getroot().iter('testcase') if c.get('classname','').endswith(('FamilyNativeTest','OorAboutNativeTest'))]
    jvm=[c for p in Path('oorarivippu/build/test-results').rglob('TEST-*FamilyProtocolTest.xml') for c in ET.parse(p).getroot().iter('testcase')]
    about=[c for p in Path('oorarivippu/build/test-results').rglob('TEST-*OorAboutGuideTest.xml') for c in ET.parse(p).getroot().iter('testcase')]
    assert len(native)==3 and len(jvm)==30 and len(about)==4, f'Required native=3 family JVM=30 About JVM=4; got {len(native)}, {len(jvm)}, {len(about)}'
    assert all(c.find(k) is None for c in native+jvm+about for k in ('failure','error','skipped'))
    root=Path(os.environ['RUNNER_TEMP'])/'health-oorarivippu';p=root/'verification.json';r=json.loads(p.read_text())
    # Re-establish only disposable emulator transport, never a physical-device root/bypass.
    assert os.environ.get('CI')=='true' and os.environ.get('GITHUB_ACTIONS')=='true'
    assert subprocess.check_output(['adb','get-serialno'],text=True).strip().startswith('emulator-')
    assert subprocess.check_output(['adb','shell','getprop','ro.kernel.qemu'],text=True).strip()=='1'
    runpy.run_path('tamilanstore/tools/signed-upgrade-smoke.py',run_name='ci_helpers')['enable_ci_root']()
    paths=subprocess.check_output(['adb','shell','pm','path',r['package']],text=True).strip().splitlines()
    assert len(paths)==1 and paths[0].startswith('package:/data/app/') and paths[0].endswith('/base.apk'), paths
    installed=paths[0].removeprefix('package:')
    # Raw, non-PTY read avoids adb sync/pull transport and an unnecessary second disk copy.
    # Still require successful transport, exact byte count AND exact inspected SHA256.
    read=subprocess.run(['adb','exec-out','cat',installed],capture_output=True,timeout=90)
    assert read.returncode==0, 'Installed APK raw read failed: '+read.stderr.decode(errors='replace')[-2000:]
    assert len(read.stdout)==r['bytes'], f"Installed APK read size {len(read.stdout)} differs from {r['bytes']}"
    actual=hashlib.sha256(read.stdout).hexdigest()
    assert actual==r['sha256'], f'Installed APK {actual} differs from inspected candidate {r["sha256"]}'
    r.update(native_tests=3,family_jvm_tests=30,about_guide_jvm_tests=4,android_api=35,exact_installed_apk_verified=True,
             bilingual_about_guide_rendered=True,encrypted_family_fixture_decrypted_after_upgrade=True,
             installed_apk_read_method="adb exec-out cat; exact raw bytes and SHA256; emulator only")
    p.write_text(json.dumps(r,indent=2)+'\n')
    print('::notice title=Verified private Oor family candidate::'+json.dumps(r))
    (root/'Oor-Arivippu-v1.1.15.apk').unlink()

if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Exact Oor native verification::'+traceback.format_exc()[-5000:].replace('%','%25').replace('\n','%0A'))
        raise
