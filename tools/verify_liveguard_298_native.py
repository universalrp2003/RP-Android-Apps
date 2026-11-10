#!/usr/bin/env python3
"""Offline XML/receipt gate AFTER emulator shutdown. No adb calls belong here."""
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET


def main():
    roots=[ET.parse(p).getroot() for p in Path('cleaner/build/outputs/androidTest-results/connected').rglob('*.xml')]
    for classname,expected in [('OfficialStatusNativeTest',5),('PillDefaultsNativeTest',4),('RainForecastDetailsNativeTest',1),('HomePlaceNativeTest',4),('CurrentPlaceNewsNativeTest',2)]:
        cases=[c for r in roots for c in r.iter('testcase') if c.get('classname','').endswith(classname)]
        assert len(cases)==expected,f'{classname}: expected {expected} tests, got {len(cases)}'
        assert all(c.find('failure') is None and c.find('error') is None and c.find('skipped') is None for c in cases),classname
    p=Path(os.environ['RUNNER_TEMP'])/'health-cleaner/verification.json'
    r=json.loads(p.read_text());assert r['final_installed_apk_sha256']==r['sha256'],'Missing exact installed-APK proof before emulator shutdown'
    r['native_tests_assert_installed_sha256']=True
    r['installed_apk_proof_stage']='before_instrumentation_and_inside_each_required_test'
    r['native_295_compact_pill_and_signal_recency']=True;r['required_android35_native_tests']=16
    r['native_296_bilingual_rain_details']=True
    r['native_297_home_management']=True
    r['native_298_current_place_news']=True
    p.write_text(json.dumps(r,indent=2)+'\n')
    print('::notice title=Required Live Guard2.98 native gates::'+json.dumps(r),flush=True)

if __name__=='__main__':
    try:main()
    except Exception:
        import traceback
        print('::error title=Native receipt gate::'+traceback.format_exc().replace('%','%25').replace('\n','%0A'),flush=True)
        raise
