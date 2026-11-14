#!/usr/bin/env python3
"""Exercise the pinned Disabler APK against a disposable user-app fixture on a stock emulator.

The only package this test asks the Disabler to change is the fixture; ADB reads state and
is used for recovery on failure. External suspension is tested on API 29+; this does not
model vendor firmware or the app's own suspension fallback.
"""
import hashlib
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
DELAY = int(os.environ.get('WAIT_BEFORE_REBOOT', '30'))
if DELAY not in (0, 15, 30):
    raise ValueError('WAIT_BEFORE_REBOOT must be 0, 15, or 30')
PKG = 'com.universalrp.appdisabler.fixture'
DISABLER = 'com.universalrp.appdisabler'


def cmd(*args, check=True):
    print('+', ' '.join(args), flush=True)
    return subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=check, timeout=60).stdout


def adb(*args, check=True):
    return cmd('adb', *args, check=check)


def dump(name):
    for _ in range(4):
        result = subprocess.run(['adb', 'shell', 'uiautomator', 'dump', '/sdcard/disabler-cycle.xml'],
                                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
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
    visible = [(n.attrib.get('text'), n.attrib.get('content-desc')) for n in last
               if n.attrib.get('text') or n.attrib.get('content-desc')]
    raise AssertionError(f'{name}: expected UI node absent; visible: {visible!r}')


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
    print('Fixture full package listing:', adb('shell', 'pm', 'list', 'packages', PKG, check=False), flush=True)
    print('Fixture dumpsys:', adb('shell', 'dumpsys', 'package', PKG, check=False), flush=True)
    raise AssertionError(f'fixture not found in pm list packages {flag}')


try:
    assert adb('get-serialno').strip().startswith('emulator-'), 'Refusing to run outside an emulator'
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    adb('shell', 'wm', 'dismiss-keyguard')
    print(adb('install', '-r', SHIZUKU))
    # Incremental installs can disappear after reboot if their loader is not persisted.
    print(adb('install', '--no-incremental', '-r', FIXTURE))
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
    # The candidate's own shell diagnostic must report the actual installed base APK.
    # Scroll the Setup card if the button is below the emulator's viewport.
    for attempt in range(5):
        nodes = dump('setup-hash-button')
        buttons = [n for n in nodes if n.attrib.get('text') == 'Check Shizuku shell']
        if buttons:
            tap(buttons[0])
            break
        adb('shell', 'input', 'swipe', '200', '520', '200', '240', '400')
        time.sleep(1)
    else:
        raise AssertionError('Check Shizuku shell button not visible')
    expected_hash = hashlib.sha256(Path(APK).read_bytes()).hexdigest()
    hash_node = wait_node('installed-hash', lambda n: 'Installed base APK SHA-256:' in n.attrib.get('text', ''))
    report = hash_node.attrib.get('text', '')
    assert 'Installed base APK SHA-256: ' + expected_hash in report, report
    assert 'Debuggable: false' in report and 'Installed splits: 0' in report, report
    (OUT / 'in-app-hash-report.txt').write_text(report + '\n')
    tap(wait_node('close-hash-report', text('Close')))
    tap(wait_node('setup', text('Apps')))
    wait_node('apps-ready', lambda n: 'Shizuku running' in n.attrib.get('text', '') and 'ready' in n.attrib.get('text', ''))
    # Restrict to user-added packages so system labels sorting before AAAA do not hide the fixture.
    # This is a navigation action only; all package changes remain fixture-only.
    tap(wait_node('apps-user-added', text('you added')))
    wait_node('fixture-before', text('AAAA Disabler Test Fixture'))
    wait_node('fixture-unlisted', text('Not listed — check first'))
    wait_node('fixture-ask-ai', text('Ask Google AI'))
    expect_state('-e', 'before')
    assert not list_contains('-d'), 'fixture unexpectedly starts disabled'
    api = int(adb('shell', 'getprop', 'ro.build.version.sdk').strip())
    if api >= 29:
        # Suspend externally: the candidate has no local note for this fixture.
        external = adb('shell', 'pm', 'suspend', '--user', '0', PKG)
        assert f'Package {PKG} new suspended state: true' in external, external
        wait_node('external-suspension-detected', text('● suspended — may still run'), timeout=80)
        # The same app can be both disabled and suspended; the row must show both.
        adb('shell', 'pm', 'disable-user', '--user', '0', PKG)
        wait_node('fixture-overlapping-states', text('● disabled and suspended — tap Enable to restore'), timeout=80)
        row_action('external-suspension-enable', 'Enable')
        deadline = time.monotonic() + 60
        while time.monotonic() < deadline:
            package_dump = adb('shell', 'dumpsys', 'package', PKG)
            if re.search(r'User 0:.*suspended=false', package_dump):
                (OUT / 'external-suspension-restored.txt').write_text(package_dump)
                break
            time.sleep(2)
        else:
            raise AssertionError('externally suspended fixture did not recover')
        expect_state('-e', 'external-restored-enabled')
        # Reload after the app finishes its asynchronous restore action.
        deadline = time.monotonic() + 60
        while time.monotonic() < deadline:
            nodes = dump('fixture-after-external-restore')
            labels = [n for n in nodes if n.attrib.get('text') == 'AAAA Disabler Test Fixture']
            buttons = [n for n in nodes if n.attrib.get('text') == 'Disable']
            if labels and any(abs(int(re.findall(r'\d+', b.attrib['bounds'])[1]) -
                                  int(re.findall(r'\d+', labels[0].attrib['bounds'])[1])) < 60 for b in buttons):
                break
            time.sleep(2)
        else:
            raise AssertionError('fixture Disable button did not return after unsuspend')
    else:
        # Stock Android 9 rejects shell suspension due to SUSPEND_APPS. Exercise ordinary
        # disable/restore and unknown display, but do not assert a suspension cycle passed.
        wait_node('external-suspension-unknown', text('● suspension status unknown'), timeout=80)
    row_action('fixture-before-disable', 'Disable')
    wait_node('unlisted-confirmation', text('Unlisted — check first'))
    tap(wait_node('unlisted-disable-anyway', text('Disable anyway')))
    expect_state('-d', 'disabled')
    print('Framework confirms fixture disabled by app', flush=True)
    wait_node('fixture-after-disable', text('AAAA Disabler Test Fixture'))
    # Vary the delay explicitly to investigate immediate-reboot persistence.
    # ADB reboot is not identical to testing the physical power menu.
    print(f'Waiting {DELAY} seconds before ADB reboot', flush=True)
    (OUT / 'reboot-delay-seconds.txt').write_text(str(DELAY) + '\n')
    time.sleep(DELAY)
    print(adb('reboot'))
    subprocess.run(['adb', 'wait-for-device'], check=True, timeout=180)
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        if adb('shell', 'getprop', 'sys.boot_completed', check=False).strip() == '1':
            break
        time.sleep(5)
    else:
        raise TimeoutError('Emulator did not finish rebooting')
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    adb('shell', 'wm', 'dismiss-keyguard')
    expect_state('-d', 'disabled-after-reboot')
    assert not list_contains('-e'), 'fixture is enabled after reboot'
    print('Framework confirms fixture stayed disabled after reboot', flush=True)
    print(cmd('timeout', '45', 'adb', 'shell', starter))
    print(adb('shell', 'am', 'start', '-W', '-n', DISABLER + '/.ui.MainActivity'))
    tap(wait_node('setup-after-reboot', text('Apps')))
    wait_node('apps-ready-after-reboot', lambda n: 'Shizuku running' in n.attrib.get('text', '') and 'ready' in n.attrib.get('text', ''))
    tap(wait_node('apps-any-after-reboot', text('Any')))
    # The fixture can be below the fold after the full app list loads. Focus search before typing.
    tap(wait_node('search-after-reboot', lambda n: n.attrib.get('content-desc') == 'Search'))
    tap(wait_node('search-field-after-reboot', lambda n: n.attrib.get('class') == 'android.widget.EditText'))
    adb('shell', 'input', 'text', 'AAAA')
    adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')  # hide keyboard, leave search active
    wait_node('fixture-after-reboot', text('AAAA Disabler Test Fixture'))
    row_action('fixture-before-enable', 'Enable')
    expect_state('-e', 'restored')
    assert not list_contains('-d'), 'fixture remains in disabled list'
    wait_node('fixture-after-enable', text('AAAA Disabler Test Fixture'))
    row_action_nodes = dump('fixture-restored')
    assert any(n.attrib.get('text') == 'Disable' for n in row_action_nodes), 'Disable button missing after restore'
    # Compare installed base APK with the exact APK downloaded from the candidate run.
    installed_paths = adb('shell', 'pm', 'path', DISABLER).splitlines()
    assert len(installed_paths) == 1 and installed_paths[0].startswith('package:'), installed_paths
    pulled = OUT / 'installed-candidate.apk'
    adb('pull', installed_paths[0].removeprefix('package:'), str(pulled))
    expected = hashlib.sha256(Path(APK).read_bytes()).hexdigest()
    installed = hashlib.sha256(pulled.read_bytes()).hexdigest()
    pulled.unlink()
    assert installed == expected, f'installed APK hash mismatch: {installed} != {expected}'
    (OUT / 'candidate-sha256.txt').write_text(expected + '\n')
    (OUT / 'fingerprint.txt').write_text(adb('shell', 'getprop', 'ro.build.fingerprint'))
    print('Framework confirms fixture restored by app; installed candidate hash matches', flush=True)
    (OUT / 'result.txt').write_text(f'PASS: stock emulator, Shizuku, app-driven disable, {DELAY}-second wait, ADB reboot and restore of disposable user app. External suspension recovery {"tested" if api >= 29 else "not tested: shell permission denied"}; OEM firmware and app-driven suspension fallback not tested.\n')
finally:
    # Safety net on the disposable emulator only; never use this as a test success.
    print(adb('shell', 'pm', 'unsuspend', '--user', '0', PKG, check=False), flush=True)
    print(adb('shell', 'pm', 'enable', '--user', '0', PKG, check=False), flush=True)
    (OUT / 'runtime.log').write_text(adb('logcat', '-d', '-v', 'brief', '-s', 'AndroidRuntime:E', check=False))
