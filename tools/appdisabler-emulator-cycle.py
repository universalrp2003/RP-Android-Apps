#!/usr/bin/env python3
"""Exercise the pinned Disabler APK against a disposable user-app fixture on a stock emulator.

The only package this test asks the Disabler to change is the fixture; ADB reads state and
is used for recovery on failure. This cannot model vendor firmware or a suspend fallback.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

APK, SHIZUKU, FIXTURE, EVIDENCE = sys.argv[1:]
OUT = Path(EVIDENCE)
OUT.mkdir(parents=True, exist_ok=True)
PKG = 'com.universalrp.appdisabler.fixture'
DISABLER = 'com.universalrp.appdisabler'


def cmd(*args, check=True):
    print('+', ' '.join(args), flush=True)
    return subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=check).stdout


def adb(*args, check=True):
    return cmd('adb', *args, check=check)


def dump(name):
    for _ in range(4):
        result = subprocess.run(['adb', 'shell', 'uiautomator', 'dump', '/sdcard/disabler-cycle.xml'],
                                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        if result.returncode == 0:
            xml = adb('exec-out', 'cat', '/sdcard/disabler-cycle.xml')
            try:
                root = ET.fromstring(xml)
                (OUT / (name + '.xml')).write_text(xml)
                return list(root.iter('node'))
            except ET.ParseError:
                pass
        print('UI dump retry:', result.stdout, flush=True)
        time.sleep(3)
    raise RuntimeError('Could not capture UI hierarchy')


def tap(node):
    coords = list(map(int, re.findall(r'\d+', node.attrib['bounds'])))
    assert len(coords) == 4, node.attrib
    x1, y1, x2, y2 = coords
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))


def wait_node(name, predicate, timeout=50):
    end = time.monotonic() + timeout
    last = []
    while time.monotonic() < end:
        last = dump(name)
        matches = [n for n in last if predicate(n)]
        if matches:
            return matches[0]
        time.sleep(2)
    raise AssertionError(f'{name}: expected UI node absent; visible: '
                         + repr([(n.attrib.get('text'), n.attrib.get('content-desc')) for n in last]))


def text(value):
    return lambda n: n.attrib.get('text') == value


def row_action(name, button):
    nodes = dump(name)
    labels = [n for n in nodes if n.attrib.get('text') == 'AAAA Disabler Test Fixture']
    assert len(labels) == 1, f'fixture not visible in {name}: ' + repr([n.attrib.get('text') for n in nodes])
    label_y = list(map(int, re.findall(r'\d+', labels[0].attrib['bounds'])))[1]
    choices = [n for n in nodes if n.attrib.get('text') == button]
    assert choices, f'no {button} button: ' + repr([n.attrib.get('text') for n in nodes])
    selected = min(choices, key=lambda n: abs(int(re.findall(r'\d+', n.attrib['bounds'])[1]) - label_y))
    distance = abs(int(re.findall(r'\d+', selected.attrib['bounds'])[1]) - label_y)
    assert distance < 140, f'{button} is not on fixture row: distance {distance}'
    tap(selected)


def list_contains(flag):
    return 'package:' + PKG in adb('shell', 'pm', 'list', 'packages', flag).splitlines()


def expect_state(flag, name):
    for _ in range(15):
        if list_contains(flag):
            (OUT / (name + '.txt')).write_text(adb('shell', 'dumpsys', 'package', PKG))
            return
        time.sleep(2)
    raise AssertionError(f'fixture not found in pm list packages {flag}')


try:
    assert adb('get-serialno').strip().startswith('emulator-'), 'Refusing to run outside an emulator'
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    adb('shell', 'wm', 'dismiss-keyguard')
    print(adb('install', '-r', SHIZUKU))
    print(adb('install', '-r', FIXTURE))
    print(adb('install', '-r', APK))
    # Starting the Shizuku manager initializes it before running its own starter as shell.
    print(adb('shell', 'monkey', '-p', 'moe.shizuku.privileged.api', '1'))
    time.sleep(5)
    package_info = adb('shell', 'dumpsys', 'package', 'moe.shizuku.privileged.api')
    (OUT / 'shizuku-package.txt').write_text(package_info)
    # The official ADB command executes the starter from the installed app's native lib directory.
    # Android 15/16 dumpsys does not expose ApplicationInfo.nativeLibraryDir by that name.
    path_lines = adb('shell', 'pm', 'path', 'moe.shizuku.privileged.api').splitlines()
    apk_path = next(line.removeprefix('package:') for line in path_lines if line.endswith('/base.apk'))
    starter = apk_path.rsplit('/', 1)[0] + '/lib/x86_64/libshizuku.so'
    print(adb('shell', 'ls', '-l', starter))
    print('Shizuku starter:', starter, flush=True)
    print(cmd('timeout', '45', 'adb', 'shell', starter))
    print(adb('shell', 'am', 'start', '-W', '-n', DISABLER + '/.ui.MainActivity'))
    # Grant the candidate's requested permission in the real Shizuku dialog, if shown.
    allow = wait_node('shizuku-permission', text('Allow all the time'))
    tap(allow)
    tap(wait_node('setup', text('Apps')))
    wait_node('apps-ready', lambda n: 'Shizuku running' in n.attrib.get('text', ''))
    # The fixture's unique label sorts first. Do not target any system package.
    tap(wait_node('apps', text('Any')))
    wait_node('fixture-before', text('AAAA Disabler Test Fixture'))
    expect_state('-e', 'before')
    assert not list_contains('-d'), 'fixture unexpectedly starts disabled'
    row_action('fixture-before-disable', 'Disable')
    expect_state('-d', 'disabled')
    print('Framework confirms fixture disabled by app', flush=True)
    wait_node('fixture-after-disable', text('AAAA Disabler Test Fixture'))
    row_action('fixture-before-enable', 'Enable')
    expect_state('-e', 'restored')
    assert not list_contains('-d'), 'fixture remains in disabled list'
    wait_node('fixture-after-enable', text('AAAA Disabler Test Fixture'))
    print('Framework confirms fixture restored by app', flush=True)
    (OUT / 'result.txt').write_text('PASS: stock emulator, Shizuku, app-driven disable and restore of disposable user app. OEM firmware and suspension fallback not tested.\n')
finally:
    # Safety net on the disposable emulator only; never use this as a test success.
    print(adb('shell', 'pm', 'enable', '--user', '0', PKG, check=False), flush=True)
    (OUT / 'runtime.log').write_text(adb('logcat', '-d', '-v', 'brief', '-s', 'AndroidRuntime:E', check=False))
