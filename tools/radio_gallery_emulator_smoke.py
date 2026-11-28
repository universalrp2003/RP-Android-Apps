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
    # The window can be between frames or covered by a system dialog at boot.
    # Never parse a stale dump from a previous screen.
    error = None
    for _ in range(8):
        try:
            adb('shell', 'rm', '-f', '/sdcard/radio-window.xml')
            adb('shell', 'uiautomator', 'dump', '/sdcard/radio-window.xml')
            return ET.fromstring(adb('exec-out', 'cat', '/sdcard/radio-window.xml'))
        except (subprocess.CalledProcessError, ET.ParseError) as exc:
            error = exc
            time.sleep(1)
    raise AssertionError('Could not retrieve emulator UI hierarchy') from error


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
# The screenshot spelling differs from the seed name. It must find the retained
# unverified record without falsely presenting the reference as a confirmed identity.
search = next((node for node in tree().iter('node') if node.get('class') == 'android.widget.EditText'), None)
assert search is not None, 'Station search field not found'
tap(search)
adb('shell', 'input', 'keyevent', 'KEYCODE_MOVE_END')
for _ in range(16):
    adb('shell', 'input', 'keyevent', 'KEYCODE_DEL')
adb('shell', 'input', 'text', 'Nattrinai')
adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
tap(wait_for('Natrinai FM'))
wait_for('Nattrinai')
wait_for('not confirmed', 'உறுதி செய்யப்படவில்லை')
wait_for('Stream not verified', 'சரிபார்க்கப்படாத ஒலிபரப்பு')
print('PASS: gallery, credits, artist navigation and unverified screenshot-name lookup on emulator.')
print('No audible playback, phone, sustained stream, identity or rights test was performed.')
