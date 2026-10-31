import re
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

class StoreCertificateRegression(unittest.TestCase):
    def test_store_pin_matches_release_preparation_not_retired_identity(self):
        prepare = (ROOT/'tools/prepare_release_signing.py').read_text()
        catalog = (ROOT/'tamilanstore/src/main/java/com/universalrp/tamilanstore/Catalog.kt').read_text()
        pin = re.search(r'EXPECTED_SIGNER = "([a-f0-9]{64})"', prepare).group(1)
        self.assertIn(f'const val SIGNER = "{pin}"', catalog)
        self.assertNotIn('9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55', catalog)

    def test_bundled_fallback_uses_verified_current_release_receipts(self):
        bundled = json.loads((ROOT/"tamilanstore/src/main/assets/catalog.json").read_text())
        published = json.loads((ROOT/"site/catalog-v2.json").read_text())
        self.assertEqual(published, bundled)

    def test_published_store_matches_the_exact_verified_release(self):
        text = (ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        self.assertIn('tagPrefix = "tamilan-store", version = "0.7", publishedVersion = "0.7"', text)
        site = (ROOT/'site/index.html').read_text()
        self.assertIn('tamilan-store-v0.7/Tamilan-Store-v0.7.apk', site)
        self.assertIn('b6618ea5e101c23ce4aada39b28820e9840bd1eede03140ff2582255fe4fd8fe', site)

    def test_installer_handoff_keeps_uri_read_only_and_failures_typed(self):
        source = ROOT/'tamilanstore/src/main/java/com/universalrp/tamilanstore'
        ui = (source/'MainActivity.kt').read_text()
        handoff = (source/'InstallerHandoff.kt').read_text()
        self.assertIn('model.verifiedFile(app)', ui)
        self.assertIn('InstallerHandoff.intent', ui)
        self.assertIn('FLAG_GRANT_READ_URI_PERMISSION', handoff)
        self.assertIn('ClipData.newRawUri', handoff)
        self.assertNotIn('FLAG_GRANT_WRITE_URI_PERMISSION', handoff)
        failures = (source/'TransferFailure.kt').read_text()
        for stage in ('HASH', 'SIZE', 'SIGNER', 'INSTALLED_SIGNER', 'DOWNGRADE'):
            self.assertIn(stage, failures)
        self.assertIn('Do not uninstall or clear app data', failures)
