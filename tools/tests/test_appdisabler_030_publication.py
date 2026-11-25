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
                             ('0.5.0', '1f9846d8d995520530ea1d9b2602daf16f8856c4371191c8d0561cba3d97b920', 11685145, 'https://github.com/universalrp2003/RP-Android-Apps/releases/download/system-app-disabler-v0.5.0/System-App-Disabler-v0.5.0.apk'))
        pages = (ROOT / '.github/workflows/pages.yml').read_text()
        self.assertIn('("system-app-disabler-v0.5.0", "System-App-Disabler-v0.5.0.apk", 11685145, "1f9846d8d995520530ea1d9b2602daf16f8856c4371191c8d0561cba3d97b920")', pages)
        dates = json.loads((ROOT / 'site/release-dates.json').read_text())
        self.assertEqual(dates['releases']['system-app-disabler-v0.5.0'], '2026-11-25T04:31:29Z')
        site = (ROOT / 'site/index.html').read_text()
        self.assertIn('1f9846d8d995520530ea1d9b2602daf16f8856c4371191c8d0561cba3d97b920', site)
        self.assertIn('recovery is not guaranteed', site)
        self.assertIn('a suspended app may still run', site)
