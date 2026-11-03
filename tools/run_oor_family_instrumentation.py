#!/usr/bin/env python3
"""Run the real AndroidJUnitRunner without AGP's post-test app uninstall/repackaging.

Keep verbatim synthetic-test evidence; construct JUnit only from matching actual start/success events.
"""
import json,os,subprocess
from pathlib import Path
import xml.etree.ElementTree as ET
assert os.environ.get('CI')=='true' and os.environ.get('GITHUB_ACTIONS')=='true'
assert subprocess.check_output(['adb','get-serialno'],text=True).strip().startswith('emulator-')
root=Path(os.environ['RUNNER_TEMP'])/'health-oorarivippu'
apks=list(Path('oorarivippu/build/outputs/apk/androidTest/release').glob('*.apk'));assert len(apks)==1
subprocess.run(['adb','install',str(apks[0])],check=True)
r=subprocess.run(['adb','shell','am','instrument','-w','-r','com.oorarivippu.voiceannouncer.test/androidx.test.runner.AndroidJUnitRunner'],capture_output=True,text=True,timeout=240)
raw=r.stdout+r.stderr;(root/'native-instrumentation.txt').write_text(raw)
expected={'nativeKeystoreStoresNoPlaintextAndDetectsTampering','twoLogicalPhonesRequireBothConfirmAndSeparateConsentAndStopRevokes'}
started=[];finished=[];event={}
for line in raw.splitlines():
    if line.startswith('INSTRUMENTATION_STATUS: '):
        key,sep,value=line.removeprefix('INSTRUMENTATION_STATUS: ').partition('=')
        if sep:event[key]=value
    elif line.startswith('INSTRUMENTATION_STATUS_CODE: '):
        code=int(line.split(':',1)[1]);assert code in (0,1), 'Native test failed/skipped/crashed; see synthetic instrumentation evidence'
        assert event.get('class')=='com.oorarivippu.voiceannouncer.family.FamilyNativeTest'
        assert event.get('test') in expected and event.get('numtests')=='2'
        (started if code==1 else finished).append(event['test']);event={}
assert r.returncode==0 and 'OK (2 tests)' in raw and 'INSTRUMENTATION_CODE: -1' in raw
assert len(started)==len(finished)==2 and set(started)==set(finished)==expected
suite=ET.Element('testsuite',name='RealAndroidJUnitRunner.FamilyNativeTest',tests='2',failures='0',errors='0',skipped='0')
for name in finished:ET.SubElement(suite,'testcase',classname='com.oorarivippu.voiceannouncer.family.FamilyNativeTest',name=name)
out=Path('oorarivippu/build/outputs/androidTest-results/connected/real-runner-android35.xml');out.parent.mkdir(parents=True,exist_ok=True)
ET.ElementTree(suite).write(out,encoding='utf-8',xml_declaration=True)
print('PASS: both actual AndroidJUnitRunner tests started and passed; raw instrument evidence retained; target APK not uninstalled')
