#!/usr/bin/env python3
"""Exercise the gallery on a disposable emulator, without publication or audible assertions."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = 'com.universalrp.tamilnadufm'


def adb(*args):
    return subprocess.run(['adb', *args], text=True, capture_output=True, check=True).stdout


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/radio-window.xml')
    return ET.fromstring(adb('exec-out', 'cat', '/sdcard/radio-window.xml'))


def find(root, text):
    return next((node for node in root.iter('node') if text in (node.get('text', '') + ' ' + node.get('content-desc', ''))), None)


def tap(node):
    bounds = [int(n) for n in re.findall(r'\d+', node.get('bounds', ''))]
    if len(bounds) != 4:
        raise AssertionError('no clickable bounds')
    adb('shell', 'input', 'tap', str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))


def wait_for(*texts, seconds=25):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        root = tree()
        for text in texts:
            node = find(root, text)
            if node is not None:
                return node
        time.sleep(1)
    print('Visible UI:', ' | '.join(n.get('text', '') for n in root.iter('node') if n.get('text')))
    raise AssertionError('Did not find any of: ' + ', '.join(texts))


adb('install', '-r', 'radio/build/outputs/apk/debug/radio-debug.apk')
adb('shell', 'pm', 'grant', PACKAGE, 'android.permission.POST_NOTIFICATIONS')
adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
time.sleep(5)
root = tree()
for prompt in ('Not now', 'பிறகு'):
    node = find(root, prompt)
    if node is not None:
        tap(node)
        break
wait_for('Ramesh Radio', 'ரமேஷ் ரேடியோ')
node = wait_for('Artists & popular songs', 'கலைஞர்கள் & பிரபலமான பாடல்கள்')
tap(node)
wait_for('Photo credits', 'பட உதவி')
wait_for('Ilaiyaraaja')
tap(wait_for('Photo credits', 'பட உதவி'))
wait_for('Sriram Narasimhan')
tap(wait_for('OK', 'சரி'))
tap(wait_for('Ilaiyaraaja'))
wait_for('ilayaraja')
print('PASS: launch, artist gallery, photo credits and artist search navigation on emulator.')
print('No audible playback, phone, sustained stream, identity or rights test was performed.')
