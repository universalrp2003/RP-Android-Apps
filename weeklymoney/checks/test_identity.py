"""Source contract checks only; these do not replace Android compilation/device tests."""
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID = '{http://schemas.android.com/apk/res/android}'


class IdentityChecks(unittest.TestCase):
    def test_seventeenth_stable_update_identity(self):
        identity = json.loads((ROOT / 'weeklymoney/app-identity.json').read_text())
        self.assertEqual(identity['portfolioNumber'], 17)
        self.assertEqual(identity['publishedVersion'], '1.7')
        self.assertEqual(identity['releaseUrl'], 'https://github.com/universalrp2003/RP-Android-Apps/releases/tag/weekly-money-v1.7')
        self.assertEqual(identity['updatePackage'], 'com.universalrp.weeklymoney.preview.notebook')
        self.assertEqual(identity['plannedProductionPackage'], identity['updatePackage'])
        self.assertEqual(identity['signerSha256'], '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d')
        self.assertEqual(identity['previewPackage'], 'com.universalrp.weeklymoney.preview')
        build = (ROOT / 'weeklymoney/app/build.gradle.kts').read_text()
        self.assertIn('applicationId = "' + identity['previewPackage'] + '"', build)

    def test_offline_and_no_platform_backup(self):
        root = ET.parse(ROOT / 'weeklymoney/app/src/main/AndroidManifest.xml').getroot()
        self.assertEqual({p.get(ANDROID + 'name') for p in root.findall('uses-permission')},
                         {'android.permission.USE_BIOMETRIC', 'android.permission.INTERNET'})
        app = root.find('application')
        self.assertEqual(app.get(ANDROID + 'allowBackup'), 'false')
        self.assertEqual(app.get(ANDROID + 'fullBackupContent'), 'false')
        entry = app.find("meta-data[@" + ANDROID + "name='com.universalrp.portfolio.NUMBER']")
        self.assertEqual(entry.get(ANDROID + 'value'), '17')

    def test_system_biometric_label_is_clean_and_demo_stays_distinct(self):
        build = (ROOT / 'weeklymoney/app/build.gradle.kts').read_text()
        self.assertIn('manifestPlaceholders["weeklymoneyLabel"] = if (sampleBuild) "வார வசூல் · DEMO" else "வார வசூல்"', build)
        manifest = ET.parse(ROOT / 'weeklymoney/app/src/main/AndroidManifest.xml').getroot()
        self.assertEqual(manifest.find('application').get(ANDROID + 'label'), '${weeklymoneyLabel}')
        activity = (ROOT / 'weeklymoney/app/src/main/java/com/universalrp/weeklymoney/MainActivity.kt').read_text()
        self.assertIn('BuildConfig.VERSION_NAME', activity)
        self.assertNotIn('Test preview', activity)
        self.assertNotIn('Collector exchange is unfinished', activity)
        self.assertIn('isDebuggable = false', build)

    def test_resources_parse(self):
        for path in (ROOT / 'weeklymoney/app/src/main/res').rglob('*.xml'):
            ET.parse(path)

    def test_dedicated_module(self):
        settings = (ROOT / 'settings.gradle.kts').read_text()
        self.assertEqual(settings.count('include(":weeklymoney")'), 1)
        self.assertIn('file("weeklymoney/app")', settings)


if __name__ == '__main__':
    unittest.main()
