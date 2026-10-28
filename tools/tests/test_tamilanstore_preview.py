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
            'android.permission.INTERNET', 'android.permission.REQUEST_INSTALL_PACKAGES'})
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
                     '== app.packageName', '== app.version', '== Catalog.SIGNER', 'newCode >= oldCode'):
            self.assertIn(gate, text)
        self.assertIn('ensureActive()', text)
        self.assertIn('instanceFollowRedirects = false', text)
        ui = (STORE / 'src/main/java/com/universalrp/tamilanstore/MainActivity.kt').read_text()
        self.assertIn('model.verifiedFile(app)', ui)
        self.assertIn('canRequestPackageInstalls()', ui)
        self.assertNotIn('PackageInstaller.Session', ui)

if __name__ == '__main__':
    unittest.main()
