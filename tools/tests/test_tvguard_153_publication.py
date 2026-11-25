import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHA = '1d7452ce651551c1e1fd5c4a4dcc73764a6f317ac58a1d69cb71bd9e719ac3c1'


class TvGuard153PublicationTest(unittest.TestCase):
    def test_catalog_and_receipt_match_exact_public_apk(self):
        receipt = json.loads((ROOT / 'docs/tvguard-v1.5.3-publication.json').read_text())
        self.assertEqual((receipt['apk_sha256'], receipt['bytes'], receipt['rebuilt']), (SHA, 11900631, False))
        for file in ('site/catalog-v2.json', 'site/catalog-v3.json', 'site/catalog-v4.json', 'tamilanstore/src/main/assets/catalog.json'):
            data = json.loads((ROOT / file).read_text())
            tv = next(a for a in data['apps'] if a['id'] == 'tvguard')
            self.assertEqual((tv['version'], tv['size'], tv['sha256'], tv['url']), ('1.5.3', receipt['bytes'], SHA, receipt['url']))
        self.assertNotIn('tvguard', {a['id'] for a in json.loads((ROOT / 'site/catalog.json').read_text())['apps']})

    def test_website_carries_limitations_and_checked_public_pin(self):
        page = (ROOT / 'site/index.html').read_text()
        guide = (ROOT / 'site/tv-guard-guide.html').read_text()
        self.assertIn(SHA, page)
        self.assertIn('Shizuku must be started separately', page)
        self.assertIn('security audit can still miss findings', guide)
        workflow = (ROOT / '.github/workflows/pages.yml').read_text()
        self.assertIn(SHA, workflow)
        self.assertIn('Android-TV-Guard-v1.5.3.apk', workflow)
