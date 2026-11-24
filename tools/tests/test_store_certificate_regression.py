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
        published = json.loads((ROOT/"site/catalog-v4.json").read_text())
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
        oor = json.loads((ROOT/'docs/oor-1.1.26-publication.json').read_text())
        self.assertEqual((old['oorarivippu']['version'], old['oorarivippu']['sha256']),
            ('1.1.26', '96ff45915749f5f1a851ff246ee9ddcd0b83886b3d1017e6ab5f22661d17d22c'))
        self.assertEqual(live['oorarivippu']['version'], '1.1.26')
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
        receipt = json.loads((ROOT/'docs/tamilan-store-0.17-publication-receipt.json').read_text())
        text = (ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        directory = re.search(r'tagPrefix = "tamilan-store", version = "([^"]+)", publishedVersion = "([^"]+)"', text)
        self.assertIsNotNone(directory)
        # 0.17 is public (24 November 2026): the directory's version and its published version are both the verified release.
        self.assertEqual(directory.group(1), '0.17')
        self.assertEqual(directory.group(2), '0.17')
        site = (ROOT/'site/index.html').read_text()
        mirrors = (ROOT/'.github/workflows/pages.yml').read_text()
        self.assertIn(receipt['url'],site)
        self.assertIn(receipt['apk_sha256'],site)
        self.assertIn(receipt['published_at'],site)
        self.assertIn(receipt['apk_sha256'],mirrors)
        self.assertEqual((receipt['release_id'],receipt['asset_id']), (456415619,786115621))
        self.assertEqual(receipt['publisher_run'],45346064452)
        self.assertEqual((receipt['run_id'],receipt['run_attempt']),(45331600676,1))
        self.assertFalse(receipt['rebuilt'])
        self.assertTrue(receipt['same_uid'])
        self.assertTrue(receipt['private_file_preserved'])
        for flag in ('retained_copy_restored_on_0_16','retained_copy_restored_after_upgrade','retained_copy_removable','refresh_enabled_after_removal'):
            self.assertTrue(receipt[flag], flag)
        # The owner installed these exact bytes before publication; Play Protect is his one-phone observation, not a clearance.
        self.assertTrue(receipt['physical_phone_tested'])
        self.assertFalse(receipt['play_protect_tested'])
        dates = json.loads((ROOT/'site/release-dates.json').read_text())
        self.assertEqual(dates['releases']['tamilan-store-v0.17'],receipt['published_at'])
        self.assertNotIn('tamilan-store-v0.16', dates['releases'])
        self.assertIn('("tamilan-store-v0.17", "Tamilan-Store-v0.17.apk", 20713347, "'+receipt['apk_sha256']+'")',mirrors)
        self.assertNotIn('("tamilan-store-v0.16",',mirrors)
        self.assertNotIn('Tamilan-Store-v0.16',site)

    def test_previous_store16_publication_receipt_stays_immutable(self):
        receipt = json.loads((ROOT/'docs/tamilan-store-0.16-publication-receipt.json').read_text())
        approved = json.loads((ROOT/'docs/approved-tamilan-store-0.16.json').read_text())
        entry = approved['publications'][0]
        for key, value in entry.items():
            self.assertEqual(receipt[key], value, key)
        self.assertEqual((receipt['release_id'],receipt['asset_id']), (455683044,783666839))
        self.assertEqual(receipt['publisher_run'],45216512529)
        self.assertFalse(receipt['rebuilt'])
        self.assertTrue(receipt['same_uid'])
        self.assertTrue(receipt['private_file_preserved'])
        self.assertTrue(receipt['prior_public_assets_unchanged'])

    def test_previous_store15_publication_receipt_stays_immutable(self):
        receipt = json.loads((ROOT/'docs/tamilan-store-0.15-publication-receipt.json').read_text())
        approved = json.loads((ROOT/'docs/approved-tamilan-store-0.15.json').read_text())
        entry = approved['publications'][0]
        for key, value in entry.items():
            self.assertEqual(receipt[key], value, key)
        self.assertEqual((receipt['release_id'],receipt['asset_id']), (455341045,782704805))
        self.assertEqual(receipt['publisher_run'],45167094585)
        self.assertFalse(receipt['rebuilt'])
        self.assertTrue(receipt['same_uid'])
        self.assertTrue(receipt['private_file_preserved'])

    def test_previous_store14_publication_receipt_stays_immutable(self):
        receipt = json.loads((ROOT/'docs/tamilan-store-0.14-publication-receipt.json').read_text())
        approved = json.loads((ROOT/'docs/approved-weekly-store-20261116.json').read_text())
        entry = next(p for p in approved['publications'] if p['app'] == 'tamilanstore')
        for key, value in entry.items():
            self.assertEqual(receipt[key], value, key)
        self.assertEqual((receipt['release_id'], receipt['asset_id']), (446868013, 754355208))
        self.assertEqual(receipt['publisher_run'], 43846064166)
        self.assertFalse(receipt['rebuilt'])
        self.assertTrue(receipt['same_uid'])
        self.assertTrue(receipt['private_file_preserved'])

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
