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
    # Small overlap matters: a large swipe can skip a short control between two tall cards.
    last=None;unchanged=0
    for attempt in range(65):
        raw,nodes=dump()
        (ROOT/'last-native-ui.xml').write_text(raw)
        visible=[]
        for node in nodes:
            bounds=list(map(int,re.findall(r'-?\d+',node.get('bounds',''))))
            if len(bounds)==4 and bounds[3]>bounds[1] and bounds[1]>=80 and bounds[3]<=1830:
                visible.append(node)
        match=next((n for n in visible if label in n.get('text','')),None)
        if match is not None:return match
        signature=tuple(n.get('text','') for n in visible if n.get('text'))
        unchanged=unchanged+1 if signature==last else 0;last=signature
        if unchanged>=3:
            break
        adb('shell','input','swipe','540','1350','540','930','220')
    print('::error title=Visible native page::'+repr([n.get('text') for n in visible if n.get('text')])[-8000:].replace('%','%25').replace('\n','%0A'))
    raise AssertionError('Native control not found: '+label)

def click(label):
    node=seek(label)
    x1,y1,x2,y2=map(int,re.findall(r'-?\d+',node.get('bounds','')))
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
    seed('weather_alert_state','<map/>')
    screen('VOICE')
    seek('News topics')
    click('Restore default topics')
    node=next(n for n in prefs('news_topics_283') if n.get('name')=='topics')
    assert len(node)==0
    # Advanced OFF is real behaviour, not merely a collapsed visual panel.
    screen('VOICE')
    click('Advanced battery checks')
    assert value('charge_announcement_v283','advanced')=='true'
    click('0.5 W');assert float(value('charge_announcement_v283','watts'))==.5
    click('50 mA');assert float(value('charge_announcement_v283','amps'))<.051
    click('3 min / நிமி');assert value('charge_announcement_v283','stable')=='180000'
    screen('VOICE')
    click('Advanced battery checks')
    assert value('charge_announcement_v283','advanced')=='false'
    screen('WEATHER')
    seek('Choose your default place first')
    seek('Town / PIN code')
    # Supply a synthetic home on this disposable emulator, without network/place search.
    adb('shell','am','force-stop',PKG)
    seed('weather_alert_state','<map><string name="saved_place_name">CI synthetic home</string><string name="saved_place_details">Test only</string><string name="saved_place_lat">10.0</string><string name="saved_place_lon">78.0</string></map>')
    adb('shell',f'chown -R {uid}:{uid} {DATA}/shared_prefs; restorecon -RF {DATA}/shared_prefs')
    screen('WEATHER')
    seek('Home · CI synthetic home')
    click('60 min');assert value('saved_rain_watches_283','lead')=='60'
    seek('Follow my current location')
    seek('Disaster reports · silent')
    click('Manage other saved places')
    seek('CI synthetic place 1')
    click('History')
    seek('Synthetic check only; not a real rain alert')
    click('Rename')
    seek('Save changes');click('Save changes')
    stored=json.loads(value('saved_rain_watches_283','places'))
    assert len(stored)==3 and stored[0]['id']==places[0]['id']
    assert adb('shell','pidof',PKG).strip()
    receipt['native_283_settings_smoke']=True
    receipt['native_283_scope']='Synthetic English UI, Advanced battery opt-in/threshold persistence, default-place-first setup, fixed home, lead time, saved-place history and screen-only disaster section; no live API, GDACS WebView, speech or phone accuracy claim'
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
