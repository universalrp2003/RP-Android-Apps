#!/usr/bin/env python3
"""Disposable emulator: exact public-to-candidate update and actual compass settings UI."""
import hashlib, os, re, subprocess, sys, time, traceback, xml.etree.ElementTree as ET

def report_failure(kind, value, tb):
    print("::error title=Compass emulator failure::" + " | ".join(traceback.format_exception(kind, value, tb)).replace("\n", "%0A"), flush=True)
    sys.__excepthook__(kind, value, tb)
sys.excepthook = report_failure
from pathlib import Path

def adb(*args):
    return subprocess.check_output(['adb', *map(str, args)], text=True, stderr=subprocess.STDOUT)

def sha(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()
assert os.environ.get('CI') == 'true' and adb('get-serialno').strip().startswith('emulator-')
base = Path(os.environ['RUNNER_TEMP'])/'oor-base.apk'
new = Path(os.environ['RUNNER_TEMP'])/'candidate/Oor-Arivippu-v1.1.26.apk'
assert sha(base) == '5d8777f1f9eb6ed8eeb87fbcf89cd1bffc30d5f7f78cca43e8f93ff7e848bd1d'
print('::notice title=Exact candidate SHA-256::'+sha(new), flush=True)
assert 'restarting adbd as root' in adb('root') or adb('shell','id').find('uid=0') >= 0
adb('wait-for-device')
pkg='com.oorarivippu.voiceannouncer'
adb('install',base)
listing=adb('shell','cmd','package','list','packages','-U',pkg)
uid=re.search(r'uid:(\d+)',listing).group(1)
data=f'/data/user/0/{pkg}'
prefs=Path(os.environ['RUNNER_TEMP'])/'oor-fixture.xml'
prefs.write_text('<map><string name="app_language">en</string><boolean name="bike_car_detection_enabled" value="false"/><string name="destination_name">Synthetic destination</string></map>')
adb('shell','mkdir','-p',data+'/shared_prefs')
adb('push',prefs,data+'/shared_prefs/oor_arivippu_prefs.xml')
adb('shell',f'chown -R {uid}:{uid} {data}/shared_prefs; restorecon -RF {data}/shared_prefs')
adb('install','-r',new)
assert re.search(r'uid:(\d+)',adb('shell','cmd','package','list','packages','-U',pkg)).group(1)==uid
assert 'versionName=1.1.26' in adb('shell','dumpsys','package',pkg)
installed=adb('shell','pm','path',pkg).strip().removeprefix('package:')
assert adb('shell','sha256sum',installed).split()[0]==sha(new), 'Installed APK differs from selected candidate'
for p in ('ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION','POST_NOTIFICATIONS'):
    adb('shell','pm','grant',pkg,'android.permission.'+p)
adb('shell','am','start','-W','-n',pkg+'/.MainActivity')
observed=[]
def nodes():
    adb('shell','uiautomator','dump','/sdcard/compass-ui.xml')
    result=list(ET.fromstring(adb('shell','cat','/sdcard/compass-ui.xml')).iter('node'))
    observed.extend((n.get('text') or '') for n in result)
    return result
def find(label, scroll=False):
    for _ in range(30):
        ns=nodes()
        matches=[n for n in ns if n.get('text')==label or n.get('content-desc')==label]
        if matches: return matches[0]
        if scroll: adb('shell','input','swipe','540','1700','540','850','600')
        time.sleep(.4)
    raise AssertionError('Missing '+label+'; visible: '+str([(n.get('text'),n.get('content-desc')) for n in nodes()]))
def tap(n):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    adb('shell','input','tap',(x1+x2)//2,(y1+y2)//2)

def readprefs():
    root=ET.fromstring(adb('shell','cat',data+'/shared_prefs/oor_arivippu_prefs.xml'))
    return {n.get('name'): n.get('value') if n.tag!='string' else n.text for n in root}
find('Family · optional town sharing')
assert readprefs()['destination_name']=='Synthetic destination'
assert readprefs()['bike_car_detection_enabled']=='false'
tap(find('Settings'))
label=find('Compass',True)
# The switch is the nearest checkbox on the same row as the Compass label.
_,top,_,bottom=map(int,re.findall(r'\d+',label.get('bounds')))
switches=[n for n in nodes() if n.get('checkable')=='true']
near=min(switches,key=lambda n: abs((lambda v:(v[1]+v[3])//2)(list(map(int,re.findall(r'\d+',n.get('bounds')))))-(top+bottom)//2))
assert near.get('checked')=='true', ET.tostring(near,encoding='unicode')
tap(near)
for _ in range(20):
    if readprefs().get('compass_enabled')=='false': break
    time.sleep(.2)
assert readprefs()['compass_enabled']=='false'
adb('shell','input','keyevent','KEYCODE_BACK')
find('Family · optional town sharing')
assert not any('Compass sensor unavailable' in (n.get('text') or '') or 'Calibrate: move phone' in (n.get('text') or '') or 'Magnetic north' in (n.get('text') or '') for n in nodes())
adb('shell','am','force-stop',pkg)
adb('shell','am','start','-W','-n',pkg+'/.MainActivity')
find('Family · optional town sharing')
assert readprefs()['compass_enabled']=='false'
tap(find('Settings')); label=find('Compass',True)
_,top,_,bottom=map(int,re.findall(r'\d+',label.get('bounds')))
switches=[n for n in nodes() if n.get('checkable')=='true']
near=min(switches,key=lambda n: abs((lambda v:(v[1]+v[3])//2)(list(map(int,re.findall(r'\d+',n.get('bounds')))))-(top+bottom)//2))
assert near.get('checked')=='false'
tap(near)
for _ in range(20):
    if readprefs().get('compass_enabled')=='true': break
    time.sleep(.2)
assert readprefs()['compass_enabled']=='true'
print('PASS: public 1.1.22 to exact signed candidate, same UID, synthetic preferences preserved; real UI switch off/restart/on')
# Inspect real built-in About in the accepted APK, not a source-only approximation.
find('About', True)
find('Version: 1.1.26', True)
find('🔗 RP-Android-Apps Official Website', True)
find('🔗 RP-Android-Apps GitHub Repository', True)
find('Our RP Ideas apps (16)', True)
for name in [
    'SMS Guard (குறுஞ்செய்தி காவலன்)', 'Live Guard', 'Ramesh Radio (தமிழன் வானொலி)',
    'PulseEQ', 'RP Vocal Remover Studio (இசை மட்டும் வேண்டும்)', 'Secret Message',
    'Expense Tracker', 'Voice Notes', 'Vasanthi AI', 'Tamil Podcast Player', 'Elderly Launcher',
    'தினக்கணக்கு / DailyVault', 'Android TV Guard (டிவி காவலன்)',
    'System App Disabler (சிஸ்டம் செயலி முடக்கி)', 'வார வசூல் / Weekly Money', 'தமிழன் / Tamilan Store'
]:
    node=find(name, True)
    assert node.get('clickable') != 'true', f'{name} must not be a link'
find('Guide — how to use this app', True)
assert not any('Legacy Mirror Repository' in text or 'GitHub release' in text or 'Android TV release' in text for text in observed), observed
print('PASS: About shows official website/repository and 16 non-link app names; legacy link absent')
