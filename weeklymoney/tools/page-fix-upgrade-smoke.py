"""Emulator-only synthetic 1.5/1.6 -> non-debuggable 1.7 update proof. Never owner data."""
import hashlib
import os
import json
from pathlib import Path
import re
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET

PACKAGE = 'com.universalrp.weeklymoney.preview.notebook'
BASE = f'/data/user/0/{PACKAGE}'
ROOT = Path('weeklymoney/app/build')

def run(*args):
    return subprocess.check_output(args, text=True).strip()

def adb(*args):
    return run('adb', *args)

def dump():
    adb('shell', 'uiautomator', 'dump', '/sdcard/weekly-release-ui.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/weekly-release-ui.xml'))

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

with tempfile.TemporaryDirectory() as tmp:
    # Native regression left an encrypted, synthetic notebook and its known test password.
    adb('root'); adb('wait-for-device')
    encrypted = subprocess.check_output(['adb', 'exec-out', 'cat', '/sdcard/Download/WeeklyMoneySynthetic/weeklymoney-synthetic-upgrade.vvb'])
    assert len(encrypted) > 100 and encrypted.startswith(b'VVB\x01'), 'Missing encrypted synthetic fixture'
    if os.environ.get('BASELINE_APK'):
        old = Path(os.environ['BASELINE_APK'])
        baseline = 'owner-candidate1.6/code18'
        expected = '329dab3a48e73728062d4644c49f822e599173b60bdce80d6035c8ffb4c0a4f8'
    else:
        run('gh', 'release', 'download', 'weekly-money-v1.5', '--repo', 'universalrp2003/RP-Android-Apps', '--pattern', 'Weekly-Money-v1.5.apk', '--dir', tmp)
        old = Path(tmp)/'Weekly-Money-v1.5.apk'
        baseline = 'public1.5/code17'
        expected = 'b2169439987275201c2a1b85d06f57f39e8791d87a0c782bbe5694d4744a56f6'
    assert hashlib.sha256(old.read_bytes()).hexdigest() == expected
    # Only this disposable emulator fixture is uninstalled to establish the real old baseline.
    if 'package:'+PACKAGE in adb('shell', 'pm', 'list', 'packages', PACKAGE).splitlines():
        assert 'Success' in adb('uninstall', PACKAGE)
    assert 'Success' in adb('install', str(old))
    uid = adb('shell', 'stat', '-c', '%u', BASE)
    local = Path(tmp)/'synthetic.vvb'; local.write_bytes(encrypted)
    adb('push', str(local), '/data/local/tmp/weekly-synthetic.vvb')
    adb('shell', 'mkdir', '-p', BASE+'/files')
    adb('shell', 'cp', '/data/local/tmp/weekly-synthetic.vvb', BASE+'/files/notebooks.vvb')
    adb('shell', 'chown', '-R', f'{uid}:{uid}', BASE+'/files')
    adb('shell', 'restorecon', '-R', BASE+'/files')
    before = subprocess.check_output(['adb', 'exec-out', 'cat', BASE+'/files/notebooks.vvb'])
    def unlock_and_show_list():
        adb('shell', 'am', 'start', '-n', PACKAGE+'/com.universalrp.weeklymoney.MainActivity')
        for _ in range(15):
            tree = dump()
            fields = [n for n in tree.iter('node') if n.get('class') == 'android.widget.EditText']
            if len(fields) == 1: break
            time.sleep(1)
        assert len(fields) == 1, 'Existing notebook must unlock, not show new-owner setup'
        tap(fields[0]); adb('shell', 'input', 'text', 'synthetic-passphrase-only'); adb('shell', 'input', 'keyevent', '4')
        tree = dump()
        button, = [n for n in tree.iter('node') if 'Continue' in n.get('text', '')]
        tap(button)
        for _ in range(15):
            tree = dump()
            nodes = list(tree.iter('node'))
            text = ' '.join(n.get('text', '') for n in nodes)
            home = next((n for n in nodes if 'புத்தகங்கள்' in n.get('text', '')), None)
            if home is not None:
                tap(home)
                tree = dump()
                text = ' '.join(n.get('text', '') for n in tree.iter('node'))
            if 'Synthetic volume 2' in text: return text
            time.sleep(1)
        raise AssertionError('Existing synthetic notebook volume not displayed')
    # Prove that the genuine public old APK can open the schema-7 fixture first.
    unlock_and_show_list()
    adb('shell', 'am', 'force-stop', PACKAGE)
    assert subprocess.check_output(['adb', 'exec-out', 'cat', BASE+'/files/notebooks.vvb']) == before
    metadata = json.loads((ROOT/'outputs/apk/release/output-metadata.json').read_text())
    apk = ROOT/'outputs/apk/release'/metadata['elements'][0]['outputFile']
    assert 'Success' in adb('install', '-r', str(apk))
    assert adb('shell', 'stat', '-c', '%u', BASE) == uid
    assert subprocess.check_output(['adb', 'exec-out', 'cat', BASE+'/files/notebooks.vvb']) == before
    text = unlock_and_show_list()
    assert 'Check for updates' in text, 'Update check must be visible on the unlocked home of an upgraded install'
    assert 'Test preview' not in text and 'TEST 0.' not in text
    receipt = dict(baseline=baseline, candidate='candidate1.7/code19', same_uid=True,
                   encrypted_notebook_byte_identical=True, master_unlock_existing_volume=True,
                   apk_sha256=hashlib.sha256(apk.read_bytes()).hexdigest(), apk_bytes=apk.stat().st_size,
                   physical_phone=False)
    (ROOT/('release-upgrade-receipt-' + ('candidate16' if os.environ.get('BASELINE_APK') else 'public15') + '.json')).write_text(json.dumps(receipt, indent=2)+'\n')
    print('::notice title=Signed release upgrade::'+json.dumps(receipt))
