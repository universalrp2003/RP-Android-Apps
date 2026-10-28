#!/usr/bin/env python3
"""Source/manifest handoff regression gate; not APK, device or Play Protect certification."""
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
A = '{http://schemas.android.com/apk/res/android}'
MODULES = ['cleaner', 'radio', 'equalizer', 'vocalremover', 'oorarivippu', 'smsguard',
           'vasanthi', 'podcast', 'launcher', 'voicenotes', 'expensetracker', 'dailyvault']

def verify_manifest(text):
    root = ET.fromstring(text)
    permissions = {n.get(A+'name') for n in root.findall('uses-permission')}
    if permissions & {'android.permission.REQUEST_INSTALL_PACKAGES', 'android.permission.INSTALL_PACKAGES'}:
        raise ValueError('Retired package-install permission')
    for p in root.findall('application/provider'):
        if (p.get(A+'authorities') or '').endswith('.updates.files'):
            raise ValueError('Retired update APK provider')

def verify_sources():
    for module in MODULES:
        verify_manifest((ROOT/module/'src/main/AndroidManifest.xml').read_text())
    verify_manifest((ROOT/'secretmessage/wifi-test/app/src/main/AndroidManifest.xml').read_text())
    for file in ['shared/src/main/java/com/universalrp/updates/AppUpdates.kt',
                 'shared/src/main/java/com/universalrp/updates/StoreFirstUpdateNotice.kt',
                 'secretmessage/wifi-test/app/src/main/java/com/rpideas/secretmessage/wifitest/updates/AppUpdates.kt']:
        text = (ROOT/file).read_text()
        if any(s in text for s in ['ACTION_INSTALL_PACKAGE', 'FileProvider', 'fun download(', 'launchInstaller', 'canRequestPackageInstalls']):
            raise ValueError('App-local installer returned: '+file)
    for file in ['LiveGuardUpdates.kt', 'VerifiedApkDownload.kt']:
        if (ROOT/'cleaner/src/main/java/com/universalrp/cleansweep/updates'/file).exists():
            raise ValueError('Retired Live Guard downloader returned')
    store = (ROOT/'tamilanstore/src/main/java/com/universalrp/tamilanstore/StoreModel.kt').read_text()
    for gate in ['app.sha256', 'Catalog.signerFor', 'newCode >= oldCode', 'info.packageName == app.packageName']:
        if gate not in store:
            raise ValueError('Store APK verification gate missing: '+gate)
    router = (ROOT/'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt').read_text()
    for gate in ['linksSupported && matchesPublishedStore(context)', 'MATCH_DEFAULT_ONLY', 'setOf(SIGNER)', 'apk.length() != STORE_BYTES', '== STORE_APK_SHA256', 'openWebsite', '#st-']:
        if gate not in router: raise ValueError('Store route guard missing: '+gate)

if __name__ == '__main__':
    verify_sources()
    print('PASS: suite source manifests retire local APK installation; Store keeps verification gates.')
