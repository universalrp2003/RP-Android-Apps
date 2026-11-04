#!/usr/bin/env python3
"""Offline XML/receipt gate AFTER emulator shutdown. No adb calls belong here."""
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET


def main():
    roots=[ET.parse(p).getroot() for p in Path('cleaner/build/outputs/androidTest-results/connected').rglob('*.xml')]
    for classname,expected in [('OfficialStatusNativeTest',5),('PillDefaultsNativeTest',3)]:
        cases=[c for r in roots for c in r.iter('testcase') if c.get('classname','').endswith(classname)]
        assert len(cases)==expected,f'{classname}: expected {expected} tests, got {len(cases)}'
        assert all(c.find('failure') is None and c.find('error') is None and c.find('skipped') is None for c in cases),classname
    p=Path(os.environ['RUNNER_TEMP'])/'health-cleaner/verification.json'
    r=json.loads(p.read_text());assert r['final_installed_apk_sha256']==r['sha256'],'Missing exact installed-APK proof before emulator shutdown'
    r['native_294_pill_defaults_overlay']=True;r['required_android35_native_tests']=8
    p.write_text(json.dumps(r,indent=2)+'\n')
    print('::notice title=Required Live Guard2.94 native gates::'+json.dumps(r),flush=True)

if __name__=='__main__':
    try:main()
    except Exception:
        import traceback
        print('::error title=Native receipt gate::'+traceback.format_exc().replace('%','%25').replace('\n','%0A'),flush=True)
        raise
