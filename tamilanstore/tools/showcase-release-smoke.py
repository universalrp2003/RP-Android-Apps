#!/usr/bin/env python3
"""Exact signed release UI / same-key Store0.7 update. Emulator only; no owner data."""
import hashlib,json,re,subprocess,sys,time,zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
PKG='com.universalrp.tamilanstore'
OUT=Path('store-showcase-results');OUT.mkdir(exist_ok=True)
def adb(*args):return subprocess.check_output(['adb',*args],timeout=60).decode(errors='replace')
def launch():adb('shell','am','start','-W','-n',PKG+'/.MainActivity')
def nodes():
    adb('shell','uiautomator','dump','/sdcard/store-showcase.xml')
    xml=adb('shell','cat','/sdcard/store-showcase.xml');(OUT/'last-ui.xml').write_text(xml)
    return list(ET.fromstring(xml).iter('node'))
def has(ns,text):return any(text in n.get('text','') for n in ns)
def wait(predicate,label,timeout=50):
    end=time.monotonic()+timeout
    while time.monotonic()<end:
        ns=nodes()
        if predicate(ns):return ns
        time.sleep(1)
    raise AssertionError(label)
def tap_node(n):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds','')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def click(text):
    ns=wait(lambda ns:any(n.get('text')==text for n in ns),text)
    tap_node(next(n for n in ns if n.get('text')==text))
def snap(name):
    nodes();(OUT/(name+'.xml')).write_text((OUT/'last-ui.xml').read_text())
    (OUT/(name+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=30))
assert adb('get-serialno').strip().startswith('emulator-')
assert adb('shell','getprop','ro.kernel.qemu').strip()=='1'
apk=Path(sys.argv[1]);previous=Path(sys.argv[2]);digest=hashlib.sha256(apk.read_bytes()).hexdigest()
with zipfile.ZipFile(apk) as z:
    index=json.loads(z.read('assets/screenshots/index.json'))
    assert len(index)==13 and len(index['dailyvault'])==7 and len(index['smsguard'])==6
    for files in index.values():
        for f in files:assert len(z.read('assets/screenshots/'+f))>1000
adb('install',str(previous));launch();click('English')
wait(lambda ns:has(ns,'Search'),'old English UI');adb('shell','am','force-stop',PKG)
adb('install','-r',str(apk));launch()
wait(lambda ns:has(ns,'Pause scrolling text') and has(ns,'Tamilan'),'new header / retained English')
snap('01-updated-english-header')
click('Pause scrolling text');wait(lambda ns:has(ns,'Enable scrolling text'),'pause control');snap('02-paused-full-text')
adb('shell','am','force-stop',PKG);launch()
wait(lambda ns:has(ns,'Enable scrolling text'),'pause survives restart')
click('தமிழ்');wait(lambda ns:has(ns,'உரை நகர்வை இயக்கு'),'Tamil header')
snap('03-tamil-header');click('English')
# Reduced system motion must leave a readable static banner even when user enables scrolling.
adb('shell','settings','put','global','animator_duration_scale','0')
click('Enable scrolling text');wait(lambda ns:has(ns,'Pause scrolling text'),'enabled preference with reduced motion')
snap('04-system-reduced-motion')
click('Pause scrolling text')
# Header is intentionally accessible; swipe to reveal the navigation row if needed.
for _ in range(4):
    if has(nodes(),'Search'):break
    adb('shell','input','swipe','500','1500','500','750','400')
click('Search')
ns=wait(lambda ns:any(n.get('class')=='android.widget.EditText' for n in ns),'search field')
tap_node(next(n for n in ns if n.get('class')=='android.widget.EditText'))
adb('shell','input','text','DailyVault');click('Show results')
for _ in range(5):
    if any('Screenshot 1;' in n.get('content-desc','') for n in nodes()):break
    adb('shell','input','swipe','500','1500','500','700','400')
ns=wait(lambda ns:any('Screenshot 1;' in n.get('content-desc','') for n in ns),'DailyVault gallery')
snap('05-dailyvault-gallery');tap_node(next(n for n in ns if 'Screenshot 1;' in n.get('content-desc','')))
wait(lambda ns:has(ns,'App screenshot'),'expanded real screenshot');snap('06-expanded-screenshot');click('Close')
adb('shell','am','force-stop',PKG);adb('install','-r',str(apk));launch()
wait(lambda ns:has(ns,'Enable scrolling text') and has(ns,'Tamilan'),'same-key reinstall retains pause/language')
assert hashlib.sha256(apk.read_bytes()).hexdigest()==digest
result={'api':35,'apk_sha256':digest,'update_from_0_7_preserves_language':True,'pause_survives_restart_and_update':True,'tamil_english_ui':True,'system_reduced_motion_path':True,'dailyvault_gallery_and_enlarge':True,'packaged_galleries':13,'physical_phone_tested':False,'play_protect_tested':False}
(OUT/'result.json').write_text(json.dumps(result,indent=2)+'\n');print('::notice::'+json.dumps(result))
