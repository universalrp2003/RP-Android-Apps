#!/usr/bin/env python3
"""Disposable CI emulator ONLY: same-key Oor upgrade, retained settings and bilingual consent UI.

Never run on a physical phone. No clear/uninstall/downgrade or Play Protect bypass.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import runpy
import subprocess
import time
import xml.etree.ElementTree as ET

HELPERS = runpy.run_path('tamilanstore/tools/signed-upgrade-smoke.py', run_name='ci_helpers')
adb = HELPERS['adb']
PKG = 'com.oorarivippu.voiceannouncer'

def main():
    assert os.environ.get('CI') == 'true' and os.environ.get('GITHUB_ACTIONS') == 'true'
    assert adb('get-serialno').strip().startswith('emulator-')
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1'
    HELPERS['enable_ci_root']()
    root = Path(os.environ['RUNNER_TEMP']) / 'health-oorarivippu'
    p = root / 'verification.json'; receipt = json.loads(p.read_text())
    candidate = root / receipt['apk']; baseline = root / 'Oor-Arivippu-v1.1.10.apk'
    assert hashlib.sha256(candidate.read_bytes()).hexdigest() == receipt['sha256']
    assert hashlib.sha256(baseline.read_bytes()).hexdigest() == receipt['baseline_sha256']
    assert receipt['publication_authorized'] is False
    # Initial baseline fixture setup on an empty emulator. Only the following -r operation tests update.
    adb('install', str(baseline))
    def state():
        listing = adb('shell','cmd','package','list','packages','-U','--user','0',PKG)
        uid = re.search(r'^package:'+re.escape(PKG)+r'\s+uid:(\d+)\s*$',listing,re.M)
        assert uid
        return uid[1], adb('shell','dumpsys','package',PKG)
    uid, old = state(); assert 'versionCode=15' in old and 'versionName=1.1.10' in old
    data = f'/data/user/0/{PKG}'
    fixture = root / 'synthetic-upgrade-marker'; fixture.write_text('Synthetic saved Oor data; not a private location.')
    prefs = root / 'synthetic-prefs.xml'
    prefs.write_text('<map><string name="app_language">en</string><string name="voice_language">en</string><boolean name="is_tracking_enabled" value="false"/><boolean name="announce_same_city_sub_area" value="false"/><boolean name="bike_car_detection_enabled" value="false"/><int name="min_distance_meters" value="750"/><int name="speech_volume_percent" value="37"/><string name="destination_name">Synthetic saved destination</string></map>')
    adb('shell', f'mkdir -p {data}/files {data}/shared_prefs')
    adb('push',str(fixture),f'{data}/files/synthetic-upgrade-marker')
    adb('push',str(prefs),f'{data}/shared_prefs/oor_arivippu_prefs.xml')
    adb('shell', f'chown -R {uid}:{uid} {data}/files {data}/shared_prefs; restorecon -RF {data}/files {data}/shared_prefs')
    def digest(path): return adb('shell','sha256sum',path).split()[0]
    marker = f'{data}/files/synthetic-upgrade-marker'; saved = f'{data}/shared_prefs/oor_arivippu_prefs.xml'
    hashes = (digest(marker),digest(saved))
    adb('install','-r',str(candidate))
    new_uid, installed = state()
    assert new_uid == uid and 'versionCode=19' in installed and 'versionName=1.1.14' in installed
    assert (digest(marker),digest(saved)) == hashes
    # Synthetic emulator only: avoid platform permission popups. Tracking remains explicitly OFF.
    for permission in ('ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION','POST_NOTIFICATIONS'):
        adb('shell','pm','grant',PKG,'android.permission.'+permission)
    def launch():
        adb('shell','am','start','-W','-n',PKG+'/.MainActivity')
        assert adb('shell','pidof',PKG).strip()
    def nodes():
        adb('shell','uiautomator','dump','/sdcard/oor-family-ui.xml')
        return list(ET.fromstring(adb('shell','cat','/sdcard/oor-family-ui.xml')).iter('node'))
    def wait(label,scroll=False):
        until=time.monotonic()+35
        while time.monotonic()<until:
            candidates=nodes()
            found=next((n for n in candidates if n.get('text')==label),None)
            if found is None and label in ('தமிழ்','ENG'):
                found=next((n for n in candidates if 'தமிழ்' in n.get('text','') and 'ENG' in n.get('text','')),None)
            if found is not None: return found
            if scroll:
                w,h=map(int,re.findall(r'(\d+)x(\d+)',adb('shell','wm','size'))[-1])
                adb('shell','input','swipe',str(w//2),str(int(h*.67)),str(w//2),str(int(h*.43)),'400')
            time.sleep(1)
        raise AssertionError('Missing bilingual family UI: '+label)
    def click(label):
        n=wait(label); x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds','')))
        adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    launch()
    click('Family · optional town sharing');wait('Family town sharing')
    wait('Stop all my family sharing',scroll=True)
    assert not any('setup/testing is not complete' in n.get('text','') for n in nodes())
    # The feature is configured, but opening it must create neither a connection nor a share.
    assert adb('shell',f'test ! -f {data}/shared_prefs/oor_family_v1.xml && echo ABSENT').strip()=='ABSENT'
    click('Close')
    # Toggle through the real app UI, then back: bilingual consent is available without changing saved settings.
    click('தமிழ்');click('குடும்பம் · விருப்பமான ஊர் பகிர்வு');wait('குடும்ப ஊர் பகிர்வு')
    wait('எனது குடும்பப் பகிர்வு அனைத்தையும் நிறுத்து',scroll=True);click('மூடு');click('ENG')
    adb('shell','am','force-stop',PKG);launch();wait('Family · optional town sharing')
    settings={n.get('name'):(n.text if n.tag=='string' else n.get('value')) for n in ET.fromstring(adb('shell','cat',saved))}
    for key,value in {'app_language':'en','is_tracking_enabled':'false','announce_same_city_sub_area':'false','bike_car_detection_enabled':'false','min_distance_meters':'750','speech_volume_percent':'37','destination_name':'Synthetic saved destination'}.items():
        assert settings.get(key)==value, 'Saved synthetic setting changed: '+key
    assert digest(marker)==hashes[0]
    adb('shell','am','force-stop',PKG)
    receipt.update(same_uid=True,data_preserved=True,preferences_preserved=True,restart_passed=True,
                   bilingual_family_consent_rendered=True,opening_panel_did_not_connect_or_share=True,
                   update_method='adb install -r; no data clear, uninstall, downgrade or bypass')
    p.write_text(json.dumps(receipt,indent=2)+'\n')
    print('::notice title=Oor exact signed saved-data upgrade::'+json.dumps(receipt))

if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Oor native upgrade or consent UI::'+traceback.format_exc()[-5000:].replace('%','%25').replace('\n','%0A'))
        raise
