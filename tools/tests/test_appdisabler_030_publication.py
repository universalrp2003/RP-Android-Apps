import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class AppDisabler030Publication(unittest.TestCase):
    def test_published_receipt_matches_site_and_catalogs(self):
        receipt = json.loads((ROOT / 'docs/appdisabler-0.3.0-publication-receipt.json').read_text())
        self.assertEqual(receipt['candidate_run'], 43557688697)
        self.assertFalse(receipt['rebuilt'])
        self.assertEqual(receipt['apk_sha256'], 'd09ab49e57e7ac3daed7e4edac4a9d8b7fc9292ccbce6ff2a4b48065977f30a0')
        for filename in ('site/catalog-v2.json', 'tamilanstore/src/main/assets/catalog.json'):
            catalog = json.loads((ROOT / filename).read_text())
            app = next(app for app in catalog['apps'] if app['id'] == 'appdisabler')
            self.assertEqual((app['version'], app['sha256'], app['size'], app['url']),
                             ('0.4.1', 'd4a51560e1cd77eb240d2132cfad568796be5f4f642f6dfce5bcecdbd5294aea', 11684389, 'https://github.com/universalrp2003/RP-Android-Apps/releases/download/system-app-disabler-v0.4.1/System-App-Disabler-v0.4.1.apk'))
        pages = (ROOT / '.github/workflows/pages.yml').read_text()
        self.assertIn('("system-app-disabler-v0.4.1", "System-App-Disabler-v0.4.1.apk", 11684389, "d4a51560e1cd77eb240d2132cfad568796be5f4f642f6dfce5bcecdbd5294aea")', pages)
        dates = json.loads((ROOT / 'site/release-dates.json').read_text())
        self.assertEqual(dates['releases']['system-app-disabler-v0.4.1'], '2026-11-21T11:21:20Z')
        site = (ROOT / 'site/index.html').read_text()
        self.assertIn('d4a51560e1cd77eb240d2132cfad568796be5f4f642f6dfce5bcecdbd5294aea', site)
        self.assertIn('recovery is not guaranteed', site)
        self.assertIn('a suspended app may still run', site)
