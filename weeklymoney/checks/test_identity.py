"""Source contract checks only; these do not replace Android compilation/device tests."""
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID = '{http://schemas.android.com/apk/res/android}'


class IdentityChecks(unittest.TestCase):
    def test_seventeenth_preview_not_falsely_published(self):
        identity = json.loads((ROOT / 'weeklymoney/app-identity.json').read_text())
        self.assertEqual(identity['portfolioNumber'], 17)
        self.assertIsNone(identity['publishedVersion'])
        self.assertIsNone(identity['releaseUrl'])
        self.assertEqual(identity['previewPackage'], 'com.universalrp.weeklymoney.preview')
        build = (ROOT / 'weeklymoney/app/build.gradle.kts').read_text()
        self.assertIn('applicationId = "' + identity['previewPackage'] + '"', build)

    def test_offline_and_no_platform_backup(self):
        root = ET.parse(ROOT / 'weeklymoney/app/src/main/AndroidManifest.xml').getroot()
        self.assertFalse(root.findall('uses-permission'))
        app = root.find('application')
        self.assertEqual(app.get(ANDROID + 'allowBackup'), 'false')
        self.assertEqual(app.get(ANDROID + 'fullBackupContent'), 'false')
        entry = app.find("meta-data[@" + ANDROID + "name='com.universalrp.portfolio.NUMBER']")
        self.assertEqual(entry.get(ANDROID + 'value'), '17')

    def test_resources_parse(self):
        for path in (ROOT / 'weeklymoney/app/src/main/res').rglob('*.xml'):
            ET.parse(path)

    def test_dedicated_module(self):
        settings = (ROOT / 'settings.gradle.kts').read_text()
        self.assertEqual(settings.count('include(":weeklymoney")'), 1)
        self.assertIn('file("weeklymoney/app")', settings)


if __name__ == '__main__':
    unittest.main()
