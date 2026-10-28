import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
STORE = ROOT / 'tamilanstore'

class TamilanStorePreviewTest(unittest.TestCase):
    def test_catalog_has_twelve_unique_official_pinned_releases(self):
        apps = json.loads((STORE / 'src/main/assets/catalog.json').read_text())['apps']
        self.assertEqual(len(apps), 12)
        self.assertEqual(len({a['packageName'] for a in apps}), 12)
        for app in apps:
            self.assertRegex(app['sha256'], r'^[a-f0-9]{64}$')
            self.assertTrue(app['url'].startswith('https://github.com/universalrp2003/RP-Android-Apps/releases/download/'))
            self.assertGreater(app['size'], 0)

    def test_permissions_and_provider_are_scoped(self):
        root = ET.parse(STORE / 'src/main/AndroidManifest.xml').getroot()
        ns = '{http://schemas.android.com/apk/res/android}'
        self.assertEqual({e.attrib[ns+'name'] for e in root.findall('uses-permission')}, {
            'android.permission.INTERNET', 'android.permission.REQUEST_INSTALL_PACKAGES', 'android.permission.POST_NOTIFICATIONS'})
        self.assertEqual(len(root.findall('queries/package')), 12)
        self.assertEqual(root.find('application/provider').attrib[ns+'exported'], 'false')
        paths = ET.parse(STORE / 'src/main/res/xml/apk_paths.xml').getroot()
        self.assertEqual(paths[0].attrib['path'], 'verified-apks/')

    def test_preview_workflow_cannot_publish(self):
        text = (ROOT / '.github/workflows/tamilanstore-preview.yml').read_text()
        self.assertIn('contents: read', text)
        self.assertIn(':tamilanstore:testDebugUnitTest', text)
        self.assertNotIn('gh release', text)
        self.assertNotIn('contents: write', text)

    def test_verification_gates_and_no_silent_install(self):
        text = (STORE / 'src/main/java/com/universalrp/tamilanstore/StoreModel.kt').read_text()
        for gate in ('total <= app.size', 'total == app.size', '== app.sha256',
                     '== app.packageName', '== app.version', '== Catalog.signerFor(app.packageName)', 'newCode >= oldCode'):
            self.assertIn(gate, text)
        self.assertIn('ensureActive()', text)
        self.assertIn('instanceFollowRedirects = false', text)
        ui = (STORE / 'src/main/java/com/universalrp/tamilanstore/MainActivity.kt').read_text()
        self.assertIn('model.verifiedFile(app)', ui)
        self.assertIn('canRequestPackageInstalls()', ui)
        self.assertNotIn('PackageInstaller.Session', ui)

if __name__ == '__main__':
    unittest.main()

class InstalledLifecycleContractTest(unittest.TestCase):
    def test_auto_refresh_and_open_take_precedence(self):
        ui = (STORE / 'src/main/java/com/universalrp/tamilanstore/MainActivity.kt').read_text()
        for action in ('ACTION_PACKAGE_ADDED', 'ACTION_PACKAGE_REPLACED', 'ACTION_PACKAGE_REMOVED'):
            self.assertIn(action, ui)
        self.assertIn('override fun onResume()', ui)
        self.assertIn('getLaunchIntentForPackage(app.packageName)', ui)
        self.assertIn('AppStatus.UPDATE_AVAILABLE', ui)
        self.assertIn('AppStatus.NEWER_INSTALLED', ui)
        self.assertIn('FilterChip(', ui)
        self.assertIn('status == AppStatus.NOT_INSTALLED || status == AppStatus.UPDATE_AVAILABLE', ui)
    def test_model_rejects_downloading_current_or_newer(self):
        model = (STORE / 'src/main/java/com/universalrp/tamilanstore/StoreModel.kt').read_text()
        self.assertIn('status != AppStatus.NOT_INSTALLED && status != AppStatus.UPDATE_AVAILABLE', model)
        self.assertIn('newCode >= oldCode', model)
        self.assertIn('InstalledStatus.resolve(this.installed(app), app.version)', model)

class QuietNotificationsContractTest(unittest.TestCase):
    def test_opt_in_silent_and_rate_limited_snapshot(self):
        text = (STORE / 'src/main/java/com/universalrp/tamilanstore/UpdateNotifications.kt').read_text()
        for check in ('getBoolean("enabled",false)', '24,TimeUnit.HOURS', 'now-last<TimeUnit.HOURS.toMillis(24)',
                      'setSound(null,null)', 'setSilent(true)', 'updates.isEmpty()', 'AppStatus.UPDATE_AVAILABLE',
                      'POST_NOTIFICATIONS', 'cancelUniqueWork'):
            self.assertIn(check, text)
        self.assertNotIn('https://', text)  # no claimed live network feed
    def test_about_and_email_contact(self):
        ui=(STORE/'src/main/java/com/universalrp/tamilanstore/MainActivity.kt').read_text()
        self.assertIn('if (showAbout)',ui)
        self.assertIn('mailto:universalrp2003@gmail.com',ui)
        self.assertIn('Intent.ACTION_SENDTO',ui)

class PerAppSignerPinTest(unittest.TestCase):
    def test_secret_message_uses_its_established_certificate(self):
        pin=(ROOT/'secretmessage/signing/certificate-sha256.txt').read_text().strip()
        text=(STORE/'src/main/java/com/universalrp/tamilanstore/Catalog.kt').read_text()
        self.assertIn(pin,text)
        self.assertIn('packageName == "com.rpideas.secretmessage"',text)
        for name in ('StoreModel.kt','StoreInventory.kt'):
            self.assertIn('Catalog.signerFor(app.packageName)',(STORE/'src/main/java/com/universalrp/tamilanstore'/name).read_text())
