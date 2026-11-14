#!/usr/bin/env python3
"""CI-only Shizuku-backed debug experiment with one disposable fixture."""
import re
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET

APK, SHIZUKU, FIXTURE, MODE = sys.argv[1:]
assert MODE in ('disable-sync', 'suspend-flush')
PKG = 'com.universalrp.appdisabler.fixture'
APP = 'com.universalrp.appdisabler'


def cmd(*args, check=True, timeout=60):
    result = subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
    print('+', *args, 'exit', result.returncode, '\n', result.stdout[-3000:], flush=True)
    if check and result.returncode:
        raise RuntimeError(f'command failed: {args}')
    return result.stdout


def adb(*args, **kwargs):
    return cmd('adb', *args, **kwargs)


def wait_permission():
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        result = subprocess.run(['adb', 'shell', 'uiautomator', 'dump', '/sdcard/lab.xml'],
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
        if result.returncode == 0:
            xml = adb('exec-out', 'cat', '/sdcard/lab.xml')
            try:
                nodes = ET.fromstring(xml).iter('node')
                for node in nodes:
                    if node.attrib.get('text') == 'Allow all the time':
                        b = list(map(int, re.findall(r'\d+', node.attrib['bounds'])))
                        adb('shell', 'input', 'tap', str((b[0]+b[2])//2), str((b[1]+b[3])//2))
                        return
            except ET.ParseError:
                pass
        time.sleep(2)
    raise RuntimeError('Shizuku permission dialog did not appear')


def reboot():
    adb('reboot')
    adb('wait-for-device', timeout=180)
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        if adb('shell', 'getprop', 'sys.boot_completed', check=False).strip() == '1':
            adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
            adb('shell', 'wm', 'dismiss-keyguard')
            return
        time.sleep(5)
    raise TimeoutError('emulator boot incomplete')


def experiment(mode):
    nonce = uuid.uuid4().hex
    adb('shell', 'am', 'start', '-W', '-n', APP + '/.lab.PersistenceLabActivity',
        '--es', 'mode', mode, '--es', 'nonce', nonce)
    deadline = time.monotonic() + 60
    marker = f'LAB_RESULT|{nonce}|'
    while time.monotonic() < deadline:
        log = adb('logcat', '-d', '-v', 'brief', '-s', 'DisablerPersistLab:I', '*:S')
        for line in log.splitlines():
            if marker in line:
                assert marker + 'OK|' + mode in line, line
                return
        time.sleep(1)
    raise TimeoutError('debug lab activity did not report completion')


def state(expected):
    if MODE == 'disable-sync':
        flag = '-d' if expected else '-e'
        assert 'package:' + PKG in adb('shell', 'pm', 'list', 'packages', flag).splitlines()
    else:
        dump = adb('shell', 'dumpsys', 'package', PKG)
        assert re.search(r'User 0:.*suspended=' + str(expected).lower(), dump), 'unexpected suspension state'


assert adb('get-serialno').strip().startswith('emulator-'), 'refusing non-emulator target'
adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
adb('shell', 'wm', 'dismiss-keyguard')
adb('install', '-r', SHIZUKU)
adb('install', '--no-incremental', '-r', FIXTURE)
adb('install', '-r', APK)
adb('shell', 'monkey', '-p', 'moe.shizuku.privileged.api', '1')
time.sleep(5)
paths = adb('shell', 'pm', 'path', 'moe.shizuku.privileged.api').splitlines()
apk = next(p.removeprefix('package:') for p in paths if p.endswith('/base.apk'))
starter = apk.rsplit('/', 1)[0] + '/lib/x86_64/libshizuku.so'
try:
    cmd('timeout', '45', 'adb', 'shell', starter)
    adb('shell', 'am', 'start', '-W', '-n', APP + '/.ui.MainActivity')
    wait_permission()
    experiment(MODE)
    state(True)
    reboot()
    state(True)
    cmd('timeout', '45', 'adb', 'shell', starter)
    experiment('enable-sync' if MODE == 'disable-sync' else 'unsuspend-flush')
    state(False)
    reboot()
    state(False)
    print('PASS: app debug activity used Shizuku shell and APK classpath; persisted and restored', MODE, flush=True)
finally:
    adb('shell', 'pm', 'unsuspend', '--user', '0', PKG, check=False)
    adb('shell', 'pm', 'enable', '--user', '0', PKG, check=False)
