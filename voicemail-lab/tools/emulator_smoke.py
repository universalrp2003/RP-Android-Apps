#!/usr/bin/env python3
"""No calls and no real voice: verify debug lab opens, permissions and tests terminate."""
import os, re, subprocess, time, traceback, sys, xml.etree.ElementTree as ET
def exception_hook(kind, value, tb):
    print("::error title=Lab smoke failure::" + "".join(traceback.format_exception(kind, value, tb)).replace("\n", "%0A"), flush=True)
    sys.__excepthook__(kind, value, tb)
sys.excepthook = exception_hook
from pathlib import Path

def adb(*a): return subprocess.check_output(['adb',*map(str,a)],text=True,stderr=subprocess.STDOUT)
assert os.environ.get('CI') == 'true' and adb('get-serialno').strip().startswith('emulator-')
pkg='com.rpideas.voicemaillab'
adb('install',Path(os.environ['RUNNER_TEMP'])/'lab/app-debug.apk')
adb('shell','pm','grant',pkg,'android.permission.RECORD_AUDIO')
adb('shell','am','start','-W','-n',pkg+'/.MainActivity')
def nodes():
    adb('shell','uiautomator','dump','/sdcard/lab-ui.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/lab-ui.xml')).iter('node'))
def all_text(): return [n.get('text','') for n in nodes()]
def find(label):
    for _ in range(12):
        for node in nodes():
            if (node.get('text') or '').casefold() == label.casefold(): return node
        adb('shell','input','swipe',540,1600,540,750,500)
        time.sleep(.2)
    raise AssertionError('Missing '+label+'; visible: '+str(all_text()))
def tap(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    adb('shell','input','tap',(x1+x2)//2,(y1+y2)//2)
def wait_result():
    for _ in range(30):
        text=all_text()
        matches=[s for s in text if ('Test complete:' in s or 'Test could not run:' in s or 'Blocked by Android:' in s or 'No samples received.' in s)]
        if matches: return matches[0]
        time.sleep(.5)
    raise AssertionError('No bounded test result')
assert any('NOT voicemail' in s for s in all_text())
tap(find('1 · Test microphone')); print('Microphone:',wait_result())
tap(find('2 · Test caller audio (during call)')); print('Downlink without call:',wait_result())
print('PASS: lab opens and both user-initiated tests terminate; no call or caller audio was tested')
