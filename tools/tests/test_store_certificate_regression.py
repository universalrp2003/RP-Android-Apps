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
        # The accepted Store0.9 APK is NOT rebuilt for a Live Guard or Oor-only release.
        # Its verified offline fallback may lag; online catalogs use the new public receipt.
        old = {a['id']: a for a in bundled['apps']}
        live = {a['id']: a for a in published['apps']}
        self.assertEqual(set(old), set(live))
        for app in old:
            if app not in ('liveguard', 'oorarivippu'): self.assertEqual(old[app], live[app], app)
        self.assertEqual((old['liveguard']['version'], old['liveguard']['sha256']),
            ('2.82', '3964b17235b107db1c7a07d92bd5d32fbe0bf29127eafcdd97d7f50f9aa677f5'))
        receipt = json.loads((ROOT/'docs/live-guard-2.94-publication-receipt.json').read_text())
        self.assertEqual(live['liveguard']['version'], '2.94')
        self.assertEqual(live['liveguard']['sha256'], receipt['apk_sha256'])
        self.assertEqual(live['liveguard']['size'], receipt['bytes'])
        self.assertEqual(live['liveguard']['url'], receipt['url'])
        oor = json.loads((ROOT/'docs/oor-about-guide-1.1.16-publication-receipt.json').read_text())
        self.assertEqual((old['oorarivippu']['version'], old['oorarivippu']['sha256']),
            ('1.1.10', '7a526fc54ac2ed07a5f972cfa030b9eb71ac77c5d1a16151c50803e9b0d7ceaa'))
        self.assertEqual(live['oorarivippu']['version'], '1.1.16')
        for key, field in [('sha256','apk_sha256'),('size','bytes'),('url','url')]:
            self.assertEqual(live['oorarivippu'][key], oor[field])
        ledger = (ROOT/'docs/published-release-digests.md').read_text()
        for app in (old['liveguard'], live['liveguard']): self.assertIn(app['sha256'], ledger)

    def test_published_store_matches_the_exact_verified_release(self):
        text = (ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        self.assertIn('tagPrefix = "tamilan-store", version = "0.9", publishedVersion = "0.9"', text)
        site = (ROOT/'site/index.html').read_text()
        self.assertIn('tamilan-store-v0.9/Tamilan-Store-v0.9.apk', site)
        self.assertIn('6b19a94be83e47e54093f9f73f9b4798d736857fc4cd16e4e7f10c7424350bab', site)
        mirrors = (ROOT/'.github/workflows/pages.yml').read_text()
        self.assertIn('3fae24373a6ead0bab0c0b2ad56a80a3b191de7a44ca69b50561deac3eea8474', mirrors)

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
