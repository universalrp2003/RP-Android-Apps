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
        published = json.loads((ROOT/"site/catalog-v3.json").read_text())
        # The bundled fallback now carries the promoted public releases too: an offline Store
        # install must offer the same archives the website pins, not a silent older build.
        old = {a['id']: a for a in bundled['apps']}
        live = {a['id']: a for a in published['apps']}
        self.assertEqual(set(old), set(live))
        for app in old:
            self.assertEqual(old[app], live[app], app)
        self.assertEqual((old['liveguard']['version'], old['liveguard']['sha256']),
            ('3.01', 'b5850343f4788d9c499024da8ea53b534a20e317bf57b071d855e565a360174f'))
        receipt = json.loads((ROOT/'docs/live-guard-3.01-publication-receipt.json').read_text())
        self.assertEqual(live['liveguard']['version'], '3.01')
        self.assertEqual(live['liveguard']['sha256'], receipt['apk_sha256'])
        self.assertEqual(live['liveguard']['size'], receipt['bytes'])
        self.assertEqual(live['liveguard']['url'], receipt['url'])
        oor = json.loads((ROOT/'docs/oor-1.1.21-publication.json').read_text())
        self.assertEqual((old['oorarivippu']['version'], old['oorarivippu']['sha256']),
            ('1.1.21', 'e65e1bdbabebc89edbe994894eb1554ce1c7583b8cdb8940aaf0eee054975258'))
        self.assertEqual(live['oorarivippu']['version'], '1.1.21')
        for key, field in [('sha256','apk_sha256'),('size','bytes'),('url','url')]:
            self.assertEqual(live['oorarivippu'][key], oor[field])
        ledger = (ROOT/'docs/published-release-digests.md').read_text()
        for app in (old['liveguard'], live['liveguard']): self.assertIn(app['sha256'], ledger)

    def test_previous_store13_publication_receipt_stays_immutable(self):
        receipt = json.loads((ROOT/'docs/tamilan-store-0.13-publication-receipt.json').read_text())
        approved = json.loads((ROOT/'docs/approved-store-0.13-candidate.json').read_text())
        for key, value in approved.items():
            self.assertEqual(receipt[key], value, key)
        self.assertEqual((receipt['release_id'], receipt['asset_id']),
                         (443943792, 742749458))
        self.assertEqual(receipt['publisher_run'], 43387318586)
        self.assertFalse(receipt['rebuilt'])
        self.assertTrue(receipt['prior_public_assets_unchanged'])

    def test_published_store_matches_the_exact_verified_release(self):
        receipt = json.loads((ROOT/'docs/tamilan-store-0.14-publication-receipt.json').read_text())
        text = (ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        self.assertIn('tagPrefix = "tamilan-store", version = "0.14", publishedVersion = "0.14"', text)
        site = (ROOT/'site/index.html').read_text()
        mirrors = (ROOT/'.github/workflows/pages.yml').read_text()
        self.assertIn(receipt['url'],site)
        self.assertIn(receipt['apk_sha256'],site)
        self.assertIn(receipt['published_at'],site)
        self.assertIn(receipt['apk_sha256'],mirrors)
        self.assertEqual((receipt['release_id'],receipt['asset_id']), (446868013,754355208))
        self.assertEqual(receipt['publisher_run'],43846064166)
        self.assertFalse(receipt['rebuilt'])
        self.assertTrue(receipt['same_uid'])
        self.assertTrue(receipt['private_file_preserved'])
        dates = json.loads((ROOT/'site/release-dates.json').read_text())
        self.assertEqual(dates['releases']['tamilan-store-v0.14'],receipt['published_at'])
        self.assertIn('("tamilan-store-v0.14", "Tamilan-Store-v0.14.apk", 19906251, "'+receipt['apk_sha256']+'")',mirrors)
        self.assertNotIn('("tamilan-store-v0.13",',mirrors)

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
