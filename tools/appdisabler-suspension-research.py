#!/usr/bin/env python3
"""Probe suspension responses using only an inert fixture on a disposable emulator."""
import subprocess
import sys

PKG = 'com.universalrp.appdisabler.researchfixture'


def adb(*args, check=True):
    result = subprocess.run(['adb', *args], text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, timeout=60)
    print(f"adb {' '.join(args)} => exit {result.returncode}\n{result.stdout}", flush=True)
    if check and result.returncode:
        raise RuntimeError(f'adb failed: {args}')
    return result


assert adb('get-serialno').stdout.strip().startswith('emulator-'), 'emulator required'
adb('install', '--no-incremental', '-r', sys.argv[1])
try:
    normal = adb('shell', 'pm', 'suspend', '--user', '0', PKG)
    assert f'Package {PKG} new suspended state: true' in normal.stdout
    restored = adb('shell', 'pm', 'unsuspend', '--user', '0', PKG)
    assert f'Package {PKG} new suspended state: false' in restored.stdout
    # Assign only our disposable fixture the active HOME role to exercise the AOSP protection.
    adb('shell', 'cmd', 'package', 'set-home-activity', '--user', '0', PKG)
    home = adb('shell', 'cmd', 'package', 'resolve-activity', '--brief', '-a',
               'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
    assert PKG in home.stdout, 'fixture did not become active HOME; refusal test invalid'
    refused = adb('shell', 'pm', 'suspend', '--user', '0', PKG, check=False)
    assert refused.returncode == 0, 'shell behavior differs from expected AOSP observation'
    assert f'Package {PKG} new suspended state: false' in refused.stdout, 'expected protected HOME refusal'
    print('CONFIRMED: shell exit 0 can report suspended state false; exit alone is not success.')
finally:
    adb('shell', 'pm', 'unsuspend', '--user', '0', PKG, check=False)
    adb('uninstall', PKG, check=False)
