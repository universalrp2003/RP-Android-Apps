#!/usr/bin/env python3
"""Exact signed APK, Android35 emulator, synthetic messages only. NOT Play Protect/phone/MMS proof."""
import json
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = 'com.universalrp.smsguard'
OUT = Path('smsguard-role-results'); OUT.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output(['adb', *args], timeout=60).decode(errors='replace')

def launch():
    adb('shell', 'am', 'start', '-W', '-n', PKG + '/.MainActivity')

def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/smsguard-role.xml')
    xml = adb('shell', 'cat', '/sdcard/smsguard-role.xml')
    (OUT / 'last-ui.xml').write_text(xml)
    ns = list(ET.fromstring(xml).iter('node'))
    labels = [n.get('text', '') for n in ns]
    if any(("isn't responding" in t or 'keeps stopping' in t) and 'SMS Guard' in t for t in labels):
        raise AssertionError('App crashed/ANR: ' + repr(labels))
    return ns

def wait(predicate, label, timeout=40):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        ns = nodes()
        if predicate(ns): return ns
        time.sleep(1)
    raise AssertionError('Timed out: ' + label)

def has(ns, text):
    return any(text in n.get('text', '') for n in ns)

def click(text):
    ns = wait(lambda ns: any(n.get('text') == text for n in ns), text)
    n = next(n for n in ns if n.get('text') == text)
    x1,y1,x2,y2 = map(int,re.findall(r'\d+', n.get('bounds', '')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def holder():
    return adb('shell', 'cmd', 'role', 'get-role-holders', '--user', '0', 'android.app.role.SMS').strip()

def chooser(ns):
    return any('permissioncontroller' in n.get('package','') for n in ns)

def snapshot(label):
    ns = nodes()
    (OUT / (label+'.xml')).write_text(ET.tostring(ET.fromstring((OUT/'last-ui.xml').read_text()), encoding='unicode'))
    (OUT / (label+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'], timeout=30))
    return ns

apk = sys.argv[1]
adb('install', '-r', apk)
initial = holder()
assert initial != PKG, 'Fresh emulator unexpectedly already defaults to this app'
launch()
click('வேண்டாம் — உள்ளூர் குரல் மட்டும்')
ns = wait(lambda ns: has(ns, 'இயல்புநிலை SMS ஆக அமை'), 'setup button')
assert not has(ns, 'Play Protect நிலை: தீர்க்கப்படவில்லை')
click('இயல்புநிலை SMS ஆக அமை')
wait(lambda ns: has(ns, 'இயல்புநிலை SMS — MMS வரம்பு'), 'MMS disclosure')
snapshot('01-disclosure')
click('ரத்து')
assert holder() == initial, 'Declining changed default app'
click('இயல்புநிலை SMS ஆக அமை')
click('SMS மட்டும் ஏற்கிறேன் — தொடர்க')
wait(chooser, 'real Android role chooser')
snapshot('02-android-chooser')
adb('shell', 'input', 'keyevent', '4')
wait(lambda ns: has(ns, 'இயல்புநிலை SMS ஆக அமை') and not chooser(ns), 'cancel returns to app')
assert holder() == initial, 'Cancelling Android chooser changed role'
# Acknowledgment survives restart; cancellation must not force-open system Settings.
adb('shell', 'am', 'force-stop', PKG)
launch()
click('இயல்புநிலை SMS ஆக அமை')
wait(chooser, 'chooser without repeated disclosure after restart')
click('SMS Guard')
ns = nodes()
confirm = next((n.get('text') for n in ns if n.get('text','').casefold() in ('set as default','set default','set as default app')), None)
if confirm:
    click(confirm)
else:
    # Android permission controller may use a resource-ID action with a different label.
    ns = nodes()
    action = next((n for n in ns if n.get('resource-id','').endswith('/button1') and n.get('enabled')=='true'), None)
    if action:
        x1,y1,x2,y2=map(int,re.findall(r'\d+',action['bounds']))
        adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
end=time.monotonic()+25
while holder()!=PKG and time.monotonic()<end: time.sleep(1)
assert holder()==PKG, 'System did not grant ROLE_SMS via UI'
launch()
snapshot('03-default-granted')
adb('emu', 'sms', 'send', '+15551234567', 'ARENA_SMS_DEFAULT_116')
wait(lambda ns: has(ns, 'ARENA_SMS_DEFAULT_116'), 'incoming SMS persisted and visible', timeout=45)
snapshot('04-incoming-synthetic-sms')
# Normal same-key update, not uninstall/clear: role, acknowledgment and SMS must survive.
adb('install', '-r', apk)
launch()
wait(lambda ns: has(ns, 'ARENA_SMS_DEFAULT_116'), 'SMS survives normal update')
assert holder()==PKG
assert not has(nodes(), 'Play Protect நிலை: தீர்க்கப்படவில்லை')
result={'apk':Path(apk).name,'api':adb('shell','getprop','ro.build.version.sdk').strip(),
        'disclosure_decline_preserves_default':True,'real_role_dialog':True,
        'chooser_cancel_preserves_default':True,'acknowledgment_survives_restart':True,
        'role_granted_through_system_ui':True,'synthetic_sms_received':True,
        'same_key_update_preserves_role_and_sms':True,
        'play_protect_tested':False,'carrier_mms_tested':False,'physical_phone_tested':False}
(OUT/'result.json').write_text(json.dumps(result,indent=2)+'\n')
print('::notice title=SMS default-role emulator result::'+json.dumps(result),flush=True)
