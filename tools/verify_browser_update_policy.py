#!/usr/bin/env python3
"""Check browser-only APK update installation and actual merged app capabilities."""
from pathlib import Path
import argparse
import hashlib
import os
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
MODULES = ('launcher', 'podcast', 'equalizer')
FORBIDDEN = {'android.permission.REQUEST_INSTALL_PACKAGES', 'android.permission.INSTALL_PACKAGES'}


def verify_manifest(text):
    root = ET.fromstring(text)
    permissions = {node.get(ANDROID + 'name') for node in root.findall('uses-permission')}
    if not permissions:
        raise ValueError('No permissions matched; refusing to verify nothing')
    if permissions & FORBIDDEN:
        raise ValueError('Browser-update app still requests package-install capability')
    for provider in root.findall('application/provider'):
        if '.updates.files' in provider.get(ANDROID + 'authorities', ''):
            raise ValueError('Browser-update app still declares an update APK provider')


def verify_apk_permissions(output):
    permissions = set(re.findall(r"uses-permission[^:]*: name='([^']+)'", output))
    if not permissions:
        raise ValueError('No APK permissions matched; refusing to verify nothing')
    if permissions & FORBIDDEN:
        raise ValueError('Merged APK still requests package-install capability')


def verify_entry(module, source):
    if not source.strip():
        raise ValueError('Empty app entry; refusing to verify nothing')
    expected = {
        'launcher': 'AppUpdates.Control("Elderly-Launcher", automatic = true)',
        'podcast': 'AppUpdates.Control("Tamil-Podcast-Player", automatic = true)',
        'equalizer': 'AppUpdates.Control("PulseEQ", automatic = true)',
    }[module]
    if expected not in source:
        raise ValueError(f'{module} does not run the daily foreground metadata check')


def verify_workflow(source):
    command = 'python3 tools/verify_browser_update_policy.py --apk "$EQ"'
    if source.count(command) != 1:
        raise ValueError('Broad workflow must inspect exactly one actual PulseEQ APK')


def verify_sources():
    verify_workflow((ROOT / '.github/workflows/build-apk.yml').read_text())
    suite = (ROOT / 'shared/src/main/java/com/universalrp/updates/SuiteAbout.kt').read_text()
    if 'BrowserReleaseUpdates(prefix)' not in suite:
        raise ValueError('Shared About page does not use the official browser download page')
    for module in MODULES:
        verify_manifest((ROOT / module / 'src/main/AndroidManifest.xml').read_text())
        main = (ROOT / module / 'src/main/java/com/universalrp/' + Path('') / Path('')) if False else None
        entry_path = {
            'equalizer': ROOT / 'equalizer/src/main/java/com/universalrp/pulseeq/MainActivity.kt',
            'podcast': ROOT / 'podcast/src/main/java/com/universalrp/podcast/MainActivity.kt',
            'launcher': ROOT / 'launcher/src/main/java/com/universalrp/launcher/MainActivity.kt',
        }[module]
        verify_entry(module, entry_path.read_text())
    about = (ROOT / 'equalizer/src/main/java/com/universalrp/pulseeq/ui/AboutScreen.kt').read_text()
    if 'BrowserReleaseUpdates("PulseEQ")' not in about:
        raise ValueError('PulseEQ About is not routed to the official website')
    equalizer_manifest = (ROOT / 'equalizer/src/main/AndroidManifest.xml').read_text()
    if 'android.permission.INTERNET' not in equalizer_manifest:
        raise ValueError('PulseEQ is missing the update-metadata-only network permission')
    notice = (ROOT / 'shared/src/main/java/com/universalrp/updates/DailyWebsiteUpdateNotice.kt').read_text()
    if 'catalog-v2.json' not in notice or 'StoreUpdateRouter.openWebsite' not in notice:
        raise ValueError('PulseEQ update notice must validate the official catalog then open the website')
    print('PASS: launcher, podcast and PulseEQ use foreground update checks and browser downloads without installer capabilities')


def verify_apk_identity(badging, gradle):
    values = {}
    for field, pattern in [('applicationId', r'applicationId\s*=\s*"([^"\n]+)"'), ('versionName', r'versionName\s*=\s*"([^"\n]+)"'), ('versionCode', r'versionCode\s*=\s*(\d+)')]:
        matches = re.findall(pattern, gradle)
        if len(matches) != 1:
            raise ValueError(f'Expected exactly one {field}; refusing to guess APK identity')
        values[field] = matches[0]
    expected = f"package: name='{values['applicationId']}' versionCode='{values['versionCode']}' versionName='{values['versionName']}'"
    if not any(line.startswith(expected) for line in badging.splitlines()):
        raise ValueError('Actual APK package/version differs from its source configuration')
    return values


def verify_apk(apk):
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or '')
    tools = list((sdk / 'build-tools').glob('*/aapt'))
    if not tools:
        raise ValueError('aapt is required to inspect the actual merged APK')
    aapt = sorted(tools, key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.parent.name)))[-1]
    result = subprocess.check_output([str(aapt), 'dump', 'permissions', str(apk)], text=True)
    verify_apk_permissions(result)
    if "uses-permission: name='android.permission.INTERNET'" not in result:
        raise ValueError('PulseEQ APK is missing the required read-only update-check network permission')
    manifest = subprocess.check_output([str(aapt), 'dump', 'xmltree', str(apk), 'AndroidManifest.xml'], text=True)
    if not manifest.strip():
        raise ValueError('Empty APK manifest dump')
    if '.updates.files' in manifest:
        raise ValueError('Merged APK contains an update APK provider')
    module = apk.resolve().relative_to(ROOT).parts[0]
    if module not in MODULES:
        raise ValueError('Unexpected module for browser-update APK inspection')
    badging = subprocess.check_output([str(aapt), 'dump', 'badging', str(apk)], text=True)
    identity = verify_apk_identity(badging, (ROOT / module / 'build.gradle.kts').read_text())
    digest = hashlib.sha256()
    with apk.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    info = '\n'.join(f'{key}: {value}' for key, value in identity.items())
    info += f'\nSource: {os.environ.get("GITHUB_SHA", "local")}\nFile: {apk.name}\nBytes: {apk.stat().st_size}\nSHA-256: {digest.hexdigest()}\nCandidate only; not a published release or a Play Protect assessment.\n'
    (apk.parent / 'candidate-info.txt').write_text(info)
    print(info)
    print(f'PASS: actual APK {apk.name} has no package-install permission or update APK provider')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path)
    args = parser.parse_args()
    verify_sources()
    if args.apk:
        verify_apk(args.apk)
