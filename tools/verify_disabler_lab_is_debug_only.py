#!/usr/bin/env python3
"""Reject a release containing the emulator-only Shizuku persistence lab."""
from pathlib import Path
from zipfile import ZipFile

DEBUG = Path('appdisabler/app/build/outputs/apk/debug/appdisabler-debug.apk')
RELEASE = Path('appdisabler/app/build/outputs/apk/release/appdisabler-release.apk')
MARKER = b'Lcom/universalrp/appdisabler/lab/PersistenceLabActivity;'
ACTIVITY = 'com.universalrp.appdisabler.lab.PersistenceLabActivity'


def entries(path):
    with ZipFile(path) as z:
        dex = b''.join(z.read(n) for n in z.namelist() if n.endswith('.dex'))
        manifest = z.read('AndroidManifest.xml')
    return dex, manifest


debug_dex, _ = entries(DEBUG)
release_dex, release_manifest = entries(RELEASE)
assert MARKER in debug_dex, 'debug APK is missing the laboratory activity'
assert MARKER not in release_dex, 'release APK contains the laboratory activity'
for value in (ACTIVITY, '.lab.PersistenceLabActivity', 'PersistenceLabActivity'):
    assert value.encode('utf-8') not in release_manifest, 'release manifest contains lab activity'
    assert value.encode('utf-16le') not in release_manifest, 'release manifest contains lab activity'
print('PASS: emulator-only lab code is present in debug APK and absent from release APK')
