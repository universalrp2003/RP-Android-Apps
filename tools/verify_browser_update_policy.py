#!/usr/bin/env python3
"""Check the two reviewed apps' source and, in CI, actual merged APK capabilities."""
from pathlib import Path
import argparse
import os
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
MODULES = ('launcher', 'podcast')
FORBIDDEN = {'android.permission.REQUEST_INSTALL_PACKAGES', 'android.permission.INSTALL_PACKAGES'}


def verify_manifest(text):
    root = ET.fromstring(text)
    permissions = {node.get(ANDROID + 'name') for node in root.findall('uses-permission')}
    if not permissions:
        raise ValueError('No permissions matched; refusing to verify nothing')
    if permissions & FORBIDDEN:
        raise ValueError('Browser-only app still requests package-install capability')
    for provider in root.findall('application/provider'):
        if '.updates.files' in provider.get(ANDROID + 'authorities', ''):
            raise ValueError('Browser-only app still declares an update APK provider')


def verify_apk_permissions(output):
    permissions = set(re.findall(r"uses-permission[^:]*: name='([^']+)'", output))
    if not permissions:
        raise ValueError('No APK permissions matched; refusing to verify nothing')
    if permissions & FORBIDDEN:
        raise ValueError('Merged APK still requests package-install capability')


def verify_sources():
    suite = (ROOT / 'shared/src/main/java/com/universalrp/updates/SuiteAbout.kt').read_text()
    if not re.search(r'if \(appId in setOf\("podcast", "launcher"\)\)\s*\{\s*BrowserReleaseUpdates\(prefix\)', suite):
        raise ValueError('The two apps are not routed to browser-only releases')
    for module in MODULES:
        verify_manifest((ROOT / module / 'src/main/AndroidManifest.xml').read_text())
        main = (ROOT / module / f'src/main/java/com/universalrp/{module}/MainActivity.kt').read_text()
        if 'AppUpdates.Control(' in main:
            raise ValueError(f'{module} bypasses the browser-only About page')
    print('PASS: launcher and podcast use browser releases without package-install permissions/providers')


def verify_apk(apk):
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or '')
    tools = list((sdk / 'build-tools').glob('*/aapt'))
    if not tools:
        raise ValueError('aapt is required to inspect the actual merged APK')
    aapt = sorted(tools, key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.parent.name)))[-1]
    result = subprocess.check_output([str(aapt), 'dump', 'permissions', str(apk)], text=True)
    verify_apk_permissions(result)
    manifest = subprocess.check_output([str(aapt), 'dump', 'xmltree', str(apk), 'AndroidManifest.xml'], text=True)
    if not manifest.strip():
        raise ValueError('Empty APK manifest dump')
    if '.updates.files' in manifest:
        raise ValueError('Merged APK contains an update APK provider')
    print(f'PASS: actual APK {apk.name} has no package-install permission or update APK provider')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path)
    args = parser.parse_args()
    verify_sources()
    if args.apk:
        verify_apk(args.apk)
