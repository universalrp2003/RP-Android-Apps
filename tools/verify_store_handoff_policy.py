#!/usr/bin/env python3
"""Guard website-only, read-only app updates; Tamilan Store remains a separate exception."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
A = '{http://schemas.android.com/apk/res/android}'
MODULES = ['cleaner', 'radio', 'equalizer', 'vocalremover', 'oorarivippu', 'smsguard',
           'vasanthi', 'podcast', 'launcher', 'voicenotes', 'expensetracker', 'dailyvault']
FORBIDDEN = {'android.permission.REQUEST_INSTALL_PACKAGES', 'android.permission.INSTALL_PACKAGES'}


def verify_manifest(text):
    root = ET.fromstring(text)
    permissions = {node.get(A + 'name') for node in root.findall('uses-permission')}
    if permissions & FORBIDDEN:
        raise ValueError('Non-Store app still requests package-install permission')
    for provider in root.findall('application/provider'):
        if (provider.get(A + 'authorities') or '').endswith('.updates.files'):
            raise ValueError('Non-Store app still declares an update APK provider')


def verify_sources():
    for module in MODULES:
        manifest = (ROOT / module / 'src/main/AndroidManifest.xml').read_text()
        verify_manifest(manifest)
        if 'com.universalrp.tamilanstore' in manifest:
            raise ValueError(f'{module} still requests Store package visibility after moving to browser handoff')
    secret_manifest = (ROOT / 'secretmessage/wifi-test/app/src/main/AndroidManifest.xml').read_text()
    verify_manifest(secret_manifest)
    if 'com.universalrp.tamilanstore' in secret_manifest:
        raise ValueError('Secret Message still requests Store package visibility after moving to browser handoff')

    update_sources = [
        'shared/src/main/java/com/universalrp/updates/AppUpdates.kt',
        'shared/src/main/java/com/universalrp/updates/DailyWebsiteUpdateNotice.kt',
        'shared/src/main/java/com/universalrp/updates/BrowserReleaseUpdates.kt',
        'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt',
        'secretmessage/wifi-test/app/src/main/java/com/rpideas/secretmessage/wifitest/updates/AppUpdates.kt',
    ]
    for relative in update_sources:
        source = (ROOT / relative).read_text()
        for forbidden in ('ACTION_INSTALL_PACKAGE', 'FileProvider', 'launchInstaller', 'canRequestPackageInstalls'):
            if forbidden in source:
                raise ValueError(f'App-local APK installer returned: {relative}')

    # Every non-Store app checks metadata only when its main UI opens/resumes.
    required_calls = {
        'cleaner/src/main/java/com/universalrp/cleansweep/MainActivity.kt': 'AppUpdates.Control("CleanSweep", automatic = true, tamil = Lang.isTamil)',
        'radio/src/main/java/com/universalrp/tamilnadufm/MainActivity.kt': 'AppUpdates.Control("Ramesh-Radio", automatic = true)',
        'equalizer/src/main/java/com/universalrp/pulseeq/MainActivity.kt': 'AppUpdates.Control("PulseEQ", automatic = true)',
        'vocalremover/src/main/java/com/universalrp/vocalremover/MainActivity.kt': 'AppUpdates.Control("RP-Vocal-Remover-Studio", automatic = true)',
        'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/MainActivity.kt': 'AppUpdates.Control("Oor-Arivippu", automatic = true)',
        'smsguard/src/main/java/com/universalrp/smsguard/MainActivity.kt': 'AppUpdates.Control("SMS-Guard", automatic = true)',
        'vasanthi/src/main/java/com/universalrp/vasanthi/MainActivity.kt': 'AppUpdates.Control("Vasanthi-AI", automatic = true)',
        'podcast/src/main/java/com/universalrp/podcast/MainActivity.kt': 'AppUpdates.Control("Tamil-Podcast-Player", automatic = true)',
        'launcher/src/main/java/com/universalrp/launcher/MainActivity.kt': 'AppUpdates.Control("Elderly-Launcher", automatic = true)',
        'voicenotes/src/main/java/com/universalrp/voicenotes/MainActivity.kt': 'AppUpdates.Control("Voice-Notes", automatic = true)',
        'expensetracker/src/main/java/com/universalrp/expensetracker/MainActivity.kt': 'AppUpdates.Control("Expense-Tracker", automatic = true)',
        'dailyvault/src/main/java/com/universalrp/dailyvault/MainActivity.kt': 'DailyWebsiteUpdateNotice("dailyvault", tamil)',
        'secretmessage/wifi-test/app/src/main/java/com/rpideas/secretmessage/wifitest/WifiTestApp.kt': 'AppUpdates.Control("Secret-Message", automatic = true)',
    }
    for relative, required in required_calls.items():
        if required not in (ROOT / relative).read_text():
            raise ValueError(f'Daily website update check is not wired for {relative}')

    notice = (ROOT / 'shared/src/main/java/com/universalrp/updates/DailyWebsiteUpdateNotice.kt').read_text()
    for required in ('UpdateReminderPolicy.shouldCheck', 'catalog-v2.json', 'Lifecycle.Event.ON_RESUME',
                     'StoreUpdateRouter.openWebsite', 'does not download or install APKs', 'sha256'):
        if required not in notice:
            raise ValueError(f'Daily website update contract missing: {required}')
    if 'WorkManager' in notice or 'ACTION_INSTALL_PACKAGE' in notice:
        raise ValueError('Update check must remain foreground-only and must not install APKs')

    router = (ROOT / 'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt').read_text()
    if 'com.universalrp.tamilanstore' in router or 'ComponentName' in router:
        raise ValueError('Non-Store updater still routes into Tamilan Store')
    if 'fun open(context: Context, appId: String): Boolean = openWebsite(context, appId)' not in router:
        raise ValueError('App update handoff is not website-only')
    if 'SisterApps.OFFICIAL_WEBSITE_URL' not in router or '#st-' not in router:
        raise ValueError('Website handoff is missing its official app anchor')

    store_manifest = ET.fromstring((ROOT / 'tamilanstore/src/main/AndroidManifest.xml').read_text())
    store_permissions = {node.get(A + 'name') for node in store_manifest.findall('uses-permission')}
    if 'android.permission.REQUEST_INSTALL_PACKAGES' not in store_permissions:
        raise ValueError('Tamilan Store installer capability was changed by the non-Store updater migration')
    store = (ROOT / 'tamilanstore/src/main/java/com/universalrp/tamilanstore/StoreModel.kt').read_text()
    for gate in ['app.sha256', 'Catalog.signerFor', 'newCode >= oldCode', 'info.packageName == app.packageName']:
        if gate not in store:
            raise ValueError('Store APK verification gate missing: ' + gate)

    print('PASS: 13 non-Store apps use daily foreground metadata checks and browser-only updates; Store remains separate.')


if __name__ == '__main__':
    verify_sources()
