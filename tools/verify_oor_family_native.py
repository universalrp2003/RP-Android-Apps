#!/usr/bin/env python3
"""Require real native results and exact installed release APK BEFORE CI emulator shuts down."""
import hashlib,json,os,subprocess
from pathlib import Path
import xml.etree.ElementTree as ET
def main():
    native=[c for p in Path('oorarivippu/build/outputs/androidTest-results/connected').rglob('*.xml') for c in ET.parse(p).getroot().iter('testcase') if c.get('classname','').endswith('FamilyNativeTest')]
    jvm=[c for p in Path('oorarivippu/build/test-results').rglob('TEST-*FamilyProtocolTest.xml') for c in ET.parse(p).getroot().iter('testcase')]
    assert len(native)==2 and len(jvm)==17, f'Required native=2 JVM=17; got native={len(native)}, JVM={len(jvm)}'
    assert all(c.find(k) is None for c in native+jvm for k in ('failure','error','skipped'))
    root=Path(os.environ['RUNNER_TEMP'])/'health-oorarivippu';p=root/'verification.json';r=json.loads(p.read_text())
    installed=subprocess.check_output(['adb','shell','pm','path',r['package']],text=True).strip().removeprefix('package:')
    pulled=root/'installed-exact.apk';subprocess.run(['adb','pull',installed,str(pulled)],check=True)
    actual=hashlib.sha256(pulled.read_bytes()).hexdigest()
    assert actual==r['sha256'], f'Installed APK {actual} differs from inspected candidate {r["sha256"]}'
    pulled.unlink()
    r.update(native_tests=2,family_jvm_tests=17,android_api=35,exact_installed_apk_verified=True)
    p.write_text(json.dumps(r,indent=2)+'\n')
    print('::notice title=Verified private Oor family candidate::'+json.dumps(r))
    (root/'Oor-Arivippu-v1.1.10.apk').unlink()

if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Exact Oor native verification::'+traceback.format_exc()[-5000:].replace('%','%25').replace('\n','%0A'))
        raise
