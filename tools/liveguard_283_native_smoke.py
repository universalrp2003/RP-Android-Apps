#!/usr/bin/env python3
"""Native UI of exact signed candidate. Synthetic data ONLY on disposable Actions emulator."""
import hashlib
import json
import os
from pathlib import Path
import re
import runpy
import subprocess
import time
import xml.etree.ElementTree as ET

H=runpy.run_path('tamilanstore/tools/signed-upgrade-smoke.py',run_name='ci_helpers')
adb=H['adb']
PKG='com.universalrp.cleansweep'
DATA=f'/data/user/0/{PKG}'
ROOT=Path(os.environ.get('RUNNER_TEMP','/tmp'))/'health-cleaner'

def dump():
    adb('shell','uiautomator','dump','/sdcard/liveguard-283.xml')
    raw=adb('shell','cat','/sdcard/liveguard-283.xml')
    return raw,list(ET.fromstring(raw).iter('node'))

def seek(label):
    for _ in range(32):
        raw,nodes=dump()
        match=next((n for n in nodes if label in n.get('text','') and n.get('bounds')!='[0,0][0,0]'),None)
        if match is not None:
            (ROOT/'last-native-ui.xml').write_text(raw)
            return match
        adb('shell','input','swipe','540','1600','540','620','180')
    raise AssertionError('Native control not found: '+label)

def click(label):
    node=seek(label)
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds','')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    time.sleep(.25)

def prefs(name):
    return ET.fromstring(adb('shell','cat',f'{DATA}/shared_prefs/{name}.xml'))

def value(name,key):
    item=next(n for n in prefs(name) if n.get('name')==key)
    return item.get('value') or item.text

def seed(name,xml):
    file=ROOT/f'{name}-synthetic.xml';file.write_text(xml)
    adb('push',str(file),f'{DATA}/shared_prefs/{name}.xml')

def screen(name):
    adb('shell','am','force-stop',PKG)
    adb('shell','am','start','-W','-n',PKG+'/.MainActivity','-a',PKG+'.WIDGET_DETAILS','--es','widget_target_screen',name)
    time.sleep(1)

def main():
    assert os.environ.get('GITHUB_ACTIONS')=='true' and os.environ.get('CI')=='true'
    assert adb('get-serialno').strip().startswith('emulator-')
    assert adb('shell','getprop','ro.kernel.qemu').strip()=='1'
    H['enable_ci_root']()
    receipt=json.loads((ROOT/'verification.json').read_text())
    assert receipt['same_uid'] and receipt['preferences_preserved'] and receipt['data_preserved']
    uid=re.search(r'uid:(\d+)',adb('shell','cmd','package','list','packages','-U','--user','0',PKG))[1]
    adb('shell','am','force-stop',PKG)
    seed('cleansweep_state','<map><string name="app_lang">en</string></map>')
    seed('news_topics_283','<map><string name="language">ENGLISH</string><set name="topics"><string>SPORTS</string></set></map>')
    places=[dict(id=f'00000000-0000-0000-0000-{i:012d}',name=f'CI synthetic place {i}',lat=10.0+i*.2,lon=78.0+i*.2,on=False) for i in range(1,4)]
    xml=ET.Element('map')
    ET.SubElement(xml,'string',name='places').text=json.dumps(places)
    ET.SubElement(xml,'int',name='lead',value='30')
    ET.SubElement(xml,'boolean',name='current',value='false')
    ET.SubElement(xml,'string',name='history_'+places[0]['id']).text=json.dumps([dict(at=1700000000000,message='Synthetic check only; not a real rain alert')])
    seed('saved_rain_watches_283',ET.tostring(xml,encoding='unicode'))
    adb('shell',f'chown -R {uid}:{uid} {DATA}/shared_prefs; restorecon -RF {DATA}/shared_prefs')
    screen('VOICE')
    seek('News topics')
    click('Restore default topics')
    node=next(n for n in prefs('news_topics_283') if n.get('name')=='topics')
    assert len(node)==0
    # Battery controls are real Compose controls, not a screenshot-only assertion.
    seek('Battery completion announcements')
    click('0.5 W');assert float(value('charge_announcement_v283','watts'))==.5
    click('50 mA');assert float(value('charge_announcement_v283','amps'))<.051
    click('3 min / நிமி');assert value('charge_announcement_v283','stable')=='180000'
    screen('WEATHER')
    seek('Rain lead time & saved watches')
    click('60 min / நிமி');assert value('saved_rain_watches_283','lead')=='60'
    seek('CI synthetic place 1')
    click('Alert history')
    seek('Synthetic check only; not a real rain alert')
    click('Edit / rename')
    # Edits remain available at the three-place limit; no delete/re-add workaround.
    seek('Save changes');click('Save changes')
    stored=json.loads(value('saved_rain_watches_283','places'))
    assert len(stored)==3 and stored[0]['id']==places[0]['id']
    seek('Nearby cyclone watch')
    assert adb('shell','pidof',PKG).strip()
    receipt['native_283_settings_smoke']=True
    receipt['native_283_scope']='Synthetic English UI, default-topic restore, battery thresholds, lead time, saved-place edit and independent history; no live rain, cyclone, map entitlement, speech or phone accuracy claim'
    (ROOT/'verification.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print('::notice title=Live Guard 2.83 native settings::'+receipt['native_283_scope'])

if __name__=='__main__':
    try:main()
    except Exception as exc:
        import traceback
        detail=traceback.format_exc()
        if isinstance(exc,subprocess.CalledProcessError):detail+=str(exc.output)
        print('::error title=Live Guard native settings::'+detail[-6000:].replace('%','%25').replace('\n','%0A'),flush=True)
        raise
