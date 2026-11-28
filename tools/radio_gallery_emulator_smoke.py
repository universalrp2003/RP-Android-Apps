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
# The all-artists tab remains visible even deep in the stations list.
wait_for('Kalai Fm-TAMIL')
for _ in range(3):
    adb('shell', 'input', 'swipe', '350', '1100', '350', '450', '450')
    time.sleep(0.5)
assert find(tree(), 'Kalai Fm-TAMIL') is None, 'Station list did not scroll'
artists_tab = ('Artists (', 'கலைஞர்கள் (')
tap(wait_for(*artists_tab))
wait_for('Photo credits', 'பட உதவி')
wait_for('Ilaiyaraaja')
# Switching tabs preserves the station list position; system Back also returns.
tap(wait_for('Stations', 'நிலையங்கள்'))
assert find(tree(), 'Kalai Fm-TAMIL') is None, 'Station position was not preserved'
tap(wait_for(*artists_tab))
wait_for('Ilaiyaraaja')
adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
assert find(tree(), 'Kalai Fm-TAMIL') is None, 'Back did not return to the scrolled stations list'
tap(wait_for(*artists_tab))
wait_for('Ilaiyaraaja')
# Reach the end of the full, scrollable artist list, then return to its top.
for _ in range(12):
    if find(tree(), 'Santhanam') is not None:
        break
    adb('shell', 'input', 'swipe', '350', '1100', '350', '400', '400')
    time.sleep(0.4)
wait_for('Santhanam')
for _ in range(12):
    root = tree()
    if find(root, 'Photo credits') is not None or find(root, 'பட உதவி') is not None:
        break
    adb('shell', 'input', 'swipe', '350', '400', '350', '1100', '400')
    time.sleep(0.4)
wait_for('Ilaiyaraaja')
tap(wait_for('Photo credits', 'பட உதவி'))
wait_for('Sriram Narasimhan')
tap(wait_for('OK', 'சரி'))
tap(wait_for('Ilaiyaraaja'))
wait_for('ilayaraja')
adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
wait_for('Photo credits', 'பட உதவி')
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
# News must start in a news category, without inheriting the radio search.
tap(wait_for('Cancel', 'ரத்து'))
# A category tap clears the artist/search filter without requiring deletion.
tap(wait_for('All stations', 'அனைத்தும்'))
search = next((n for n in tree().iter('node') if n.get('class') == 'android.widget.EditText'), None)
assert search is not None and search.get('text', '') != 'Nattrinai', 'Category kept the old search'
# Back from the directory returns to the app, even while a request is running.
tap(wait_for('Directory', 'பட்டியல்'))
wait_for('Live directory', 'நேரடி நிலையப் பட்டியல்')
adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
wait_for('Stations', 'நிலையங்கள்')
def open_menu():
    tap(wait_for('Open navigation menu', 'பட்டியலைத் திற'))
    wait_for('Settings & more', 'அமைப்புகள் மற்றும் மேலும்')


def navigate(*labels):
    open_menu()
    root = tree()
    node = next((n for n in root.iter('node') if n.get('text', '') in labels), None)
    assert node is not None, 'Drawer navigation missing: ' + str(labels)
    tap(node)

# Back dismisses the drawer without changing destination.
open_menu()
adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
wait_for('Open navigation menu', 'பட்டியலைத் திற')
assert find(tree(), 'Settings & more') is None, 'Back did not close drawer'
navigate('News', 'செய்திகள்')
wait_for('News radio', 'செய்தி வானொலி')
adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
wait_for('Stations', 'நிலையங்கள்')
navigate('News', 'செய்திகள்')
wait_for('News radio', 'செய்தி வானொலி')
assert find(tree(), 'Nattrinai') is None, 'Radio search leaked into news'
navigate('Local music', 'உள்ளூர் பாடல்கள்')
navigate('Equalizer', 'ஒலி சமநிலை')
navigate('Settings & more', 'அமைப்புகள் மற்றும் மேலும்')
navigate('Radio', 'வானொலி')
wait_for('Station, city or language', 'நிலையம்')
assert find(tree(), 'Nattrinai') is None, 'Old search remained after switching screens'
print('PASS: gallery, artist shortcuts, drawer navigation and retained radio search on emulator.')
print('No audible playback, phone, sustained stream, identity or rights test was performed.')
