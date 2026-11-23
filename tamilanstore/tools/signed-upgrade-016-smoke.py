#!/usr/bin/env python3
"""Exact release-key Store0.16 upgrade from the public Store0.15 on a disposable CI emulator, never an owner's phone.

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
OLD_SHA = 'c9284fbf067c664bf496c39bc0dee9230b8e1510a4882757d933013d9bcfb84b'
VERIFIED_TEXT = 'File verified. Tap Install to open Android’s confirmation.'


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


DUMP = '/sdcard/store-upgrade.xml'


def ui():
    # uiautomator exits 0 with "ERROR: could not get idle state" while the screen animates and
    # leaves the previous file behind; never read a stale hierarchy, retry the dump instead.
    out = ''
    for attempt in range(15):
        adb('shell', 'rm', '-f', DUMP)
        out = adb('shell', 'uiautomator', 'dump', DUMP)
        if 'dumped' in out:
            text = adb('shell', 'cat', DUMP)
            return text, list(ET.fromstring(text).iter('node'))
        time.sleep(1)
    raise AssertionError('uiautomator produced no hierarchy: ' + out[-300:])


def retained_diagnostics():
    prefs = adb('shell', 'cat', f'/data/user/0/{PKG}/shared_prefs/active_download_v1.xml').strip()
    copies = adb('shell', 'ls', '-l', f'/data/user/0/{PKG}/cache/verified-apks').strip()
    node = wait_text('Refresh catalog')
    return ('prefs=' + prefs[-600:] + ' | copies=' + copies + ' | refresh node=' +
            ' '.join(f'{k}={v}' for k, v in sorted(node.attrib.items()) if k in ('text', 'enabled', 'clickable', 'bounds')) +
            ' | visible=' + ' || '.join(visible_texts())[:1500])


def visible_texts(limit=40):
    # Bounded evidence only: node labels of the Store's own screen, never device data.
    _, nodes = ui()
    return [n.get('text').strip() for n in nodes if n.get('text', '').strip()][:limit]

def dismiss_launcher_anr(nodes):
    """A cold emulator can raise "Pixel Launcher isn't responding" over the app under test.
    Tapping Wait keeps both alive; Close app is never touched."""
    wait = next((n for n in nodes if n.get('text') == 'Wait' and n.get('package') != PKG), None)
    if wait is None:
        return False
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', wait.get('bounds', '')))
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(1)
    return True


def wait_text_if_present(label, timeout=45):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        text, nodes = ui()
        if dismiss_launcher_anr(nodes):
            continue
        match = next((n for n in nodes if n.get('text') == label), None)
        if match is not None:
            return match
        time.sleep(1)
    return None

def wait_text(label, timeout=45):
    match = wait_text_if_present(label, timeout)
    if match is not None:
        return match
    raise AssertionError('Missing native UI text: ' + label + ' | visible: ' + ' || '.join(visible_texts())[:1200])


def scroll_until_text(label, swipes=8):
    """New apps sit below the fold of a 17-app list, so look for them by scrolling."""
    for _ in range(swipes):
        text, nodes = ui()
        if dismiss_launcher_anr(nodes):
            continue
        match = next((n for n in nodes if n.get('text') == label), None)
        if match is not None:
            return match
        adb('shell', 'input', 'swipe', '540', '1500', '540', '600', '400')
        time.sleep(0.6)
    raise AssertionError('Missing native UI text after scrolling: ' + label)


def dismiss_system_dialogs():
    """A fresh emulator can raise a notification/permission dialog over the first launch."""
    for _ in range(6):
        text, nodes = ui()
        button = next((n for n in nodes if n.get('text') in ('Allow', 'அனுமதி')), None)
        if button is None:
            return
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', button.get('bounds', '')))
        adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
        time.sleep(1)


def click_if_present(label, timeout=20):
    """Return True when the control was tapped. The caller asserts the real end state."""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        text, nodes = ui()
        node = next((n for n in nodes if n.get('text') == label), None)
        if node is not None:
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds', '')))
            adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
            return True
        time.sleep(1)
    return False


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


def download_retained_copy():
    """Through the public 0.15's own Download control, the real user path that leaves a verified
    copy behind when the installer is never opened (the user backs out, or the process dies)."""
    click('Download')
    wait_text(VERIFIED_TEXT, 240)


def refresh_control():
    node = wait_text('Refresh catalog')
    return node.get('enabled')


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
    assert previous.stat().st_size == 20713379
    assert hashlib.sha256(previous.read_bytes()).hexdigest() == OLD_SHA
    enable_ci_root()
    # Fresh fixture installation is setup on this empty emulator, not the tested upgrade.
    adb('install', str(previous))
    launch()
    dismiss_system_dialogs()
    # The language buttons only exist before a choice is stored; the assertions below are what
    # actually pin the resulting preferences.
    click_if_present('English')
    click_if_present('Pause scrolling text')
    wait_text('Enable scrolling text')
    for attempt in range(20):
        if prefs().get('tamil') == 'false' and prefs().get('store_motion_v1') == 'false':
            break
        time.sleep(0.2)
    assert prefs().get('tamil') == 'false' and prefs().get('store_motion_v1') == 'false'
    old = package_state()
    assert old['code'] == 17 and old['name'] == '0.15'
    marker = f'/data/user/0/{PKG}/files/synthetic-upgrade-marker'
    adb('shell', f'mkdir -p /data/user/0/{PKG}/files; printf synthetic-ci-only > {marker}')
    marker_hash = adb('shell', 'sha256sum', marker).split()[0]
    snap(out, '01-store15-before-upgrade')
    # Reproduce the supplied artifact's incompatibility WITHOUT changing the installation.
    rejected = subprocess.run(['adb', 'install', '-r', str(debug)], capture_output=True, text=True, timeout=90)
    diagnostic = rejected.stdout + rejected.stderr
    assert rejected.returncode != 0 and 'INSTALL_FAILED_UPDATE_INCOMPATIBLE' in diagnostic
    (out / 'debug-upgrade-rejection.txt').write_text(diagnostic)
    assert package_state() == old
    assert adb('shell', 'sha256sum', marker).split()[0] == marker_hash
    adb('shell', 'am', 'force-stop', PKG)
    # Reproduce the owner's report on the public 0.15 first: download a not-installed app through
    # the Store's own control, never open the installer, and relaunch. The retained verified copy
    # comes back and holds "Refresh catalog" disabled, with no status line explaining why.
    launch()
    wait_text('Tamilan')
    wait_text('Enable scrolling text')
    download_retained_copy()
    (out / 'retained-state-on-device.txt').write_text(adb('shell', 'cat', f'/data/user/0/{PKG}/shared_prefs/active_download_v1.xml'))
    adb('shell', 'am', 'force-stop', PKG)
    launch()
    wait_text('Tamilan')
    snap(out, '01b-store15-relaunch-with-retained-copy')
    held = None
    for attempt in range(30):
        held = refresh_control()
        if held == 'false':
            break
        time.sleep(1)
    assert held == 'false', 'expected the public 0.15 to hold Refresh catalog closed: ' + retained_diagnostics()
    assert wait_text_if_present('Last catalog refresh succeeded', 5) is None
    snap(out, '01b-store15-retained-copy-holds-refresh')
    adb('shell', 'am', 'force-stop', PKG)
    # The operation being tested: same-package, release-key, higher-version update. No reset.
    adb('install', '-r', str(candidate))
    new = package_state()
    assert new['code'] == 18 and new['name'] == '0.16'
    assert new['uid'] == old['uid']
    assert adb('shell', 'sha256sum', marker).split()[0] == marker_hash
    assert prefs().get('tamil') == 'false' and prefs().get('store_motion_v1') == 'false'
    launch()
    wait_text('Tamilan')
    wait_text('Enable scrolling text')
    assert 'Debug preview' not in ui()[0], 'Release artifact must not masquerade as a debug preview'
    # The retained copy survived the upgrade (its app is still not installed, so it is still
    # installable and 0.16 keeps it) but the Store now offers a way to let go of it.
    scroll_until_text('Remove downloaded file')
    snap(out, '02b-store16-retained-copy-offered')
    click('Remove downloaded file')
    assert wait_text_if_present('Remove downloaded file', 5) is None, 'removal must clear the retained copy'
    adb('shell', 'am', 'force-stop', PKG)
    launch()
    wait_text('Tamilan')
    wait_text('Enable scrolling text')
    # The refresh on open may already be running (the control is busy, then shows its result) or the
    # control is free for a manual refresh; the 0.15 state, idle and disabled, must be gone.
    if wait_text_if_present('Last catalog refresh succeeded', 60) is None:
        enabled = refresh_control()
        assert enabled == 'true', '0.16 must leave Refresh catalog usable: enabled=' + str(enabled)
    snap(out, '02c-store16-refresh-usable')
    # The header status is at the top, so check it before scrolling into the longer app list.
    # The first foreground refresh races the emulator's network after the reinstall; if it has not
    # succeeded within 60 s, record the status the Store showed and use the same manual "Refresh
    # catalog" control a user has. No install or data operation is repeated.
    if wait_text_if_present('Last catalog refresh succeeded', 60) is None:
        (out / 'catalog-status-before-manual-refresh.txt').write_text('\n'.join(visible_texts()) + '\n')
        click('Refresh catalog')
    wait_text('Last catalog refresh succeeded')
    click('Refresh catalog')
    wait_text('Last catalog refresh succeeded')
    snap(out, '02-store16-release-key-upgrade')
    # Previously listed apps stay, and the whole point of 0.15: the new app is listed by the updated Store itself.
    scroll_until_text('Android TV Guard')
    scroll_until_text('System App Disabler')
    scroll_until_text('Weekly Money')
    # The bundled catalog lists the app under its plain name (the "· experimental" tag was
    # dropped from the Store entry name with Animal Sounds 0.22); scroll for exactly that.
    scroll_until_text('Animal Sounds')
    snap(out, '03-store16-new-app-listed')
    # Restart must preserve the two real UI preferences as well as the synthetic file.
    adb('shell', 'am', 'force-stop', PKG)
    launch()
    wait_text('Tamilan')
    wait_text('Enable scrolling text')
    snap(out, '04-after-restart')
    assert hashlib.sha256(candidate.read_bytes()).hexdigest() == before_digest
    result = dict(from_version='0.15', to_version='0.16', version_code=18,
                  retained_copy_reproduced_on_0_15=True, retained_copy_removable=True, refresh_enabled_after_removal=True,
                  package=PKG, installation_method='adb install -r; no bypass flags',
                  debug_over_release_rejected=True, same_uid=True, private_file_preserved=True, language_preserved=True,
                  motion_preference_preserved=True, restart_passed=True, manual_catalog_refresh_rendered=True,
                  physical_phone_tested=False, play_protect_tested=False,
                  debug_to_release_upgrade_supported=False)
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    print('::notice title=Store0.16 release-key upgrade::' + json.dumps(result))


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
