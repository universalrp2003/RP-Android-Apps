#!/usr/bin/env python3
"""Exact release-key Store upgrade on a disposable CI emulator, never an owner's phone.

ADB tests Android package/signing/data continuity, NOT Play Protect or file-manager installation.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import traceback
import xml.etree.ElementTree as ET

PKG = 'com.universalrp.tamilanstore'
OLD_SHA = '6b19a94be83e47e54093f9f73f9b4798d736857fc4cd16e4e7f10c7424350bab'


def adb(*args, timeout=90):
    return subprocess.check_output(['adb', *args], stderr=subprocess.STDOUT, timeout=timeout).decode(errors='replace')


def enable_ci_root():
    # adbd can return nonzero while restarting after boot. Validate UID independently;
    # bounded infrastructure recovery only, never an install retry or a phone-root attempt.
    last = ''
    for attempt in range(5):
        try:
            adb('root', timeout=20)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
            last = str(getattr(error, 'output', b''))[-500:]
        try:
            adb('wait-for-device', timeout=30)
            if adb('shell', 'id', '-u', timeout=10).strip() == '0':
                return
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
            last = str(getattr(error, 'output', b''))[-500:]
        time.sleep(2)
    raise AssertionError('CI emulator adbd did not become root after five bounded attempts: ' + last)


def ui():
    adb('shell', 'uiautomator', 'dump', '/sdcard/store-upgrade.xml')
    text = adb('shell', 'cat', '/sdcard/store-upgrade.xml')
    return text, list(ET.fromstring(text).iter('node'))


def wait_text(label):
    deadline = time.monotonic() + 45
    while time.monotonic() < deadline:
        text, nodes = ui()
        match = next((n for n in nodes if n.get('text') == label), None)
        if match is not None:
            return match
        time.sleep(1)
    raise AssertionError('Missing native UI text: ' + label)


def click(label):
    node = wait_text(label)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds', '')))
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))


def launch():
    adb('shell', 'am', 'start', '-W', '-n', PKG + '/.MainActivity')


def prefs():
    raw = adb('shell', 'cat', f'/data/user/0/{PKG}/shared_prefs/MainActivity.xml')
    return {n.get('name'): n.get('value') for n in ET.fromstring(raw).findall('boolean')}


def parse_package_state(dump, listing):
    code = re.search(r'\bversionCode=(\d+)', dump)
    name = re.search(r'\bversionName=([^\s]+)', dump)
    # Android dumpsys changed userId/appId labels. Use the package command's explicit
    # user-0 UID listing instead; still require the exact package, never a substring match.
    uid = re.search(r'^package:' + re.escape(PKG) + r'\s+uid:(\d+)\s*$', listing, re.MULTILINE)
    assert code and name and uid, f'Cannot inspect installed package: code={bool(code)} name={bool(name)} uid={bool(uid)}'
    return {'code': int(code[1]), 'name': name[1], 'uid': uid[1]}


def package_state():
    dump = adb('shell', 'dumpsys', 'package', PKG)
    listing = adb('shell', 'cmd', 'package', 'list', 'packages', '-U', '--user', '0', PKG)
    return parse_package_state(dump, listing)


def snap(out, name):
    text, _ = ui()
    (out / (name + '.xml')).write_text(text)
    (out / (name + '.png')).write_bytes(subprocess.check_output(['adb', 'exec-out', 'screencap', '-p'], timeout=30))


def run(out):
    # No install, root, settings or application-data operation before all these guards pass.
    assert os.environ.get('CI') == 'true' and os.environ.get('GITHUB_ACTIONS') == 'true'
    assert adb('get-serialno').strip().startswith('emulator-')
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1'
    assert len(sys.argv) == 4
    candidate, previous, debug = map(Path, sys.argv[1:])
    before_digest = hashlib.sha256(candidate.read_bytes()).hexdigest()
    assert previous.stat().st_size == 19309106
    assert hashlib.sha256(previous.read_bytes()).hexdigest() == OLD_SHA
    enable_ci_root()
    # Fresh fixture installation is setup on this empty emulator, not the tested upgrade.
    adb('install', str(previous))
    launch()
    click('English')
    click('Pause scrolling text')
    wait_text('Enable scrolling text')
    for attempt in range(20):
        if prefs().get('tamil') == 'false' and prefs().get('store_motion_v1') == 'false':
            break
        time.sleep(0.2)
    assert prefs().get('tamil') == 'false' and prefs().get('store_motion_v1') == 'false'
    old = package_state()
    assert old['code'] == 11 and old['name'] == '0.9'
    marker = f'/data/user/0/{PKG}/files/synthetic-upgrade-marker'
    adb('shell', f'mkdir -p /data/user/0/{PKG}/files; printf synthetic-ci-only > {marker}')
    marker_hash = adb('shell', 'sha256sum', marker).split()[0]
    snap(out, '01-store08-before-upgrade')
    # Reproduce the supplied artifact's incompatibility WITHOUT changing the installation.
    rejected = subprocess.run(['adb', 'install', '-r', str(debug)], capture_output=True, text=True, timeout=90)
    diagnostic = rejected.stdout + rejected.stderr
    assert rejected.returncode != 0 and 'INSTALL_FAILED_UPDATE_INCOMPATIBLE' in diagnostic
    (out / 'debug-upgrade-rejection.txt').write_text(diagnostic)
    assert package_state() == old
    assert adb('shell', 'sha256sum', marker).split()[0] == marker_hash
    adb('shell', 'am', 'force-stop', PKG)
    # The operation being tested: same-package, release-key, higher-version update. No reset.
    adb('install', '-r', str(candidate))
    new = package_state()
    assert new['code'] == 12 and new['name'] == '0.10'
    assert new['uid'] == old['uid']
    assert adb('shell', 'sha256sum', marker).split()[0] == marker_hash
    assert prefs().get('tamil') == 'false' and prefs().get('store_motion_v1') == 'false'
    launch()
    wait_text('Tamilan')
    wait_text('Enable scrolling text')
    assert 'Debug preview' not in ui()[0], 'Release artifact must not masquerade as a debug preview'
    snap(out, '02-store09-release-key-upgrade')
    # Restart must preserve the two real UI preferences as well as the synthetic file.
    adb('shell', 'am', 'force-stop', PKG)
    launch()
    wait_text('Tamilan')
    wait_text('Enable scrolling text')
    snap(out, '03-after-restart')
    assert hashlib.sha256(candidate.read_bytes()).hexdigest() == before_digest
    result = dict(from_version='0.9', to_version='0.10', version_code=12,
                  package=PKG, installation_method='adb install -r; no bypass flags',
                  debug_over_release_rejected=True, same_uid=True, private_file_preserved=True, language_preserved=True,
                  motion_preference_preserved=True, restart_passed=True,
                  physical_phone_tested=False, play_protect_tested=False,
                  debug_to_release_upgrade_supported=False)
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    print('::notice title=Store0.9 release-key upgrade::' + json.dumps(result))


if __name__ == '__main__':
    out = Path(__file__).resolve().parents[2] / 'store-signed-upgrade-results'
    out.mkdir(parents=True, exist_ok=True)
    try:
        run(out)
    except Exception:
        detail = traceback.format_exc()
        (out / 'failure.txt').write_text(detail)
        print('::error title=Store exact upgrade failure::' + detail[-4500:].replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A'), flush=True)
        raise
