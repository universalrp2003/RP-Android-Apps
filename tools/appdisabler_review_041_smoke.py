#!/usr/bin/env python3
"""Disposable Android emulator only: exact update, read-only review and plan; never disable packages."""
import hashlib, os, re, subprocess, sys, time, traceback, xml.etree.ElementTree as ET
from pathlib import Path

def report(kind, value, tb):
    print('::error title=Review smoke failure::'+''.join(traceback.format_exception(kind,value,tb)).replace('%','%25').replace('\n','%0A'),flush=True)
    sys.__excepthook__(kind,value,tb)
sys.excepthook=report

def adb(*a): return subprocess.check_output(['adb', *map(str,a)], text=True, stderr=subprocess.STDOUT)
def sha(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()
assert os.environ.get('CI')=='true' and adb('get-serialno').strip().startswith('emulator-')
t=Path(os.environ['RUNNER_TEMP']); old=t/'old/System-App-Disabler-v0.3.0.apk'; new=t/'candidate/appdisabler-0.4.1.apk'
assert sha(old)=='d09ab49e57e7ac3daed7e4edac4a9d8b7fc9292ccbce6ff2a4b48065977f30a0'
pkg='com.universalrp.appdisabler'
# Install the inert synthetic package only if this emulator has no real TalkBack package.
if 'package:com.google.android.marvin.talkback' not in adb('shell','pm','list','packages','-u','com.google.android.marvin.talkback'):
    adb('install',t/'talkback-fixture/fixture.apk')
adb('install',old)
uid=re.search(r'uid:(\d+)',adb('shell','cmd','package','list','packages','-U',pkg)).group(1)
adb('install','-r',new)
assert re.search(r'uid:(\d+)',adb('shell','cmd','package','list','packages','-U',pkg)).group(1)==uid
assert 'versionName=0.4.1' in adb('shell','dumpsys','package',pkg)
installed=adb('shell','pm','path',pkg).strip().removeprefix('package:')
assert adb('shell','sha256sum',installed).split()[0]==sha(new)
adb('shell','am','start','-W','-n',pkg+'/.ui.MainActivity')
def nodes():
    adb('shell','uiautomator','dump','/sdcard/appreview-ui.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/appreview-ui.xml')).iter('node'))
def texts(): return [n.get('text','') for n in nodes()]
def find(label, substring=False, scroll=False):
    for _ in range(24):
        for n in nodes():
            s=n.get('text','')
            if (label in s if substring else s == label): return n
        if scroll: adb('shell','input','swipe',500,1550,500,850,450)
        time.sleep(.3)
    raise AssertionError('Missing '+label+'; visible '+str(texts()))
def tap(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds'))); adb('shell','input','tap',(x1+x2)//2,(y1+y2)//2)
tap(find('Apps'))
# Search by package and verify both the list and detail warning, without changing state.
def find_desc(label):
    for _ in range(20):
        for n in nodes():
            if n.get('content-desc') == label: return n
        time.sleep(.3)
    raise AssertionError('Missing description '+label+'; visible '+str(texts()))
tap(find_desc('Search'))
for n in nodes():
    if n.get('class') == 'android.widget.EditText':
        tap(n); break
else: raise AssertionError('Search text field not found')
adb('shell','input','text','com.google.android.marvin.talkback')
adb('shell','input','keyevent','KEYCODE_BACK')  # Hide the keyboard before tapping the row.
warning = find('Accessibility — important; check first',substring=True)
tap(warning)
find('Accessibility — important; check first',substring=True)
adb('shell','input','keyevent','KEYCODE_BACK')
tap(find_desc('Close search'))
tap(find('Compare / ஒப்பிடு'))
find('No apps are changed here.',substring=True)
tap(find('Save baseline / முதல் நிலை'))
find('Save baseline?'); tap(find('Save / சேமி'))
find('Baseline:',substring=True)
tap(find('Compare now / இப்போது ஒப்பிடு'))
find('enabled state changed',substring=True,scroll=True)
find('Export baseline / பட்டியலை சேமி')
tap(find('Close / மூடு'))
tap(find('+ Plan', scroll=True))
tap(find('Plan / திட்டம் (1)',substring=True)); find('Planning does not change',substring=True)
find('Remove / நீக்கு',scroll=True); tap(find('Remove / நீக்கு',scroll=True))
find('No apps selected.',substring=True)
tap(find('Close / மூடு'))
# Baseline must survive stopping and relaunching; no package action has been initiated.
adb('shell','am','force-stop',pkg); adb('shell','am','start','-W','-n',pkg+'/.ui.MainActivity')
tap(find('Apps')); tap(find('Compare / ஒப்பிடு')); find('Baseline:',substring=True)
print('PASS: public 0.3.0 → exact same-key 0.4.1, same UID and installed bytes; read-only baseline, comparison, plan and restart. No package state changes or real Shizuku tests.')
