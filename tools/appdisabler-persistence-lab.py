#!/usr/bin/env python3
"""Disposable-emulator-only immediate reboot experiment; never targets a real package."""
import os
import re
import subprocess
import sys
import time

APK, JAR, MODE = sys.argv[1:]
PKG = 'com.universalrp.persistencelab.fixture'
assert MODE in ('disable-baseline', 'disable-sync', 'suspend-baseline', 'suspend-flush')


def adb(*args, check=True, timeout=60):
    result = subprocess.run(['adb', *args], text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, timeout=timeout)
    print('+ adb', *args, 'exit', result.returncode, '\n', result.stdout, flush=True)
    if check and result.returncode:
        raise RuntimeError(f'adb command failed: {args}')
    return result.stdout


def package_dump():
    return adb('shell', 'dumpsys', 'package', PKG)


def verify():
    if MODE.startswith('disable'):
        packages = adb('shell', 'pm', 'list', 'packages', '-d').splitlines()
        assert 'package:' + PKG in packages, 'fixture is not disabled'
    else:
        dump = package_dump()
        assert re.search(r'User 0:.*suspended=true', dump), 'fixture is not suspended'


def probe(*args):
    out = adb('shell', 'CLASSPATH=/data/local/tmp/persistence-probe.jar',
              'app_process', '/system/bin', 'com.universalrp.persistencelab.Probe', *args)
    assert 'LAB_PROBE_COMPLETED ' + args[0] in out, 'probe did not finish'


assert adb('get-serialno').strip().startswith('emulator-'), 'refusing non-emulator target'
adb('install', '--no-incremental', '-r', APK)
adb('push', JAR, '/data/local/tmp/persistence-probe.jar')
try:
    if MODE == 'disable-baseline':
        adb('shell', 'pm', 'disable-user', '--user', '0', PKG)
    elif MODE == 'disable-sync':
        probe('disable-sync', PKG)
    else:
        out = adb('shell', 'pm', 'suspend', '--user', '0', PKG)
        assert f'Package {PKG} new suspended state: true' in out
        if MODE == 'suspend-flush':
            probe('flush')
    verify()
    # No artificial delay. ADB reboot is not a physical power-menu restart.
    def reboot():
        adb('reboot')
        adb('wait-for-device', timeout=180)
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            if adb('shell', 'getprop', 'sys.boot_completed', check=False).strip() == '1':
                return
            time.sleep(5)
        raise TimeoutError('boot incomplete')

    reboot()
    verify()
    print('RESULT: persisted', MODE, flush=True)
    # Test shell-level restore too, including an immediate second reboot. This is not an app test.
    if MODE == 'disable-sync':
        probe('enable-sync', PKG)
    elif MODE == 'disable-baseline':
        adb('shell', 'pm', 'enable', '--user', '0', PKG)
    else:
        out = adb('shell', 'pm', 'unsuspend', '--user', '0', PKG)
        assert f'Package {PKG} new suspended state: false' in out
        if MODE == 'suspend-flush':
            probe('flush')

    def verify_restored():
        if MODE.startswith('disable'):
            assert 'package:' + PKG in adb('shell', 'pm', 'list', 'packages', '-e').splitlines(), 'fixture not enabled'
        else:
            assert re.search(r'User 0:.*suspended=false', package_dump()), 'fixture remains suspended'

    verify_restored()
    reboot()
    verify_restored()
    print('RESULT: PASS persisted and restored', MODE, flush=True)
finally:
    adb('shell', 'pm', 'unsuspend', '--user', '0', PKG, check=False)
    adb('shell', 'pm', 'enable', '--user', '0', PKG, check=False)
    adb('uninstall', PKG, check=False)
