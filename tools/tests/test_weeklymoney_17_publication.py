import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SHA = '0dac276f391fb4099fe13ac45d67adfade6fe245b644d634a87a6b6af1cbc81a'
URL = 'https://github.com/universalrp2003/RP-Android-Apps/releases/download/weekly-money-v1.7/Weekly-Money-v1.7.apk'

class WeeklyMoney17PublicationTest(unittest.TestCase):
    def test_receipt_matches_all_current_catalogs_and_public_website(self):
        receipt = json.loads((ROOT/'docs/weekly-money-1.7-publication-receipt.json').read_text())
        self.assertEqual((receipt['version'], receipt['version_code'], receipt['apk_sha256'], receipt['url']), ('1.7', 19, SHA, URL))
        self.assertTrue(receipt['owner_reported_phone_test'])
        self.assertFalse(receipt['phone_apk_hash_verified'])
        self.assertFalse(receipt['rebuilt'])
        for path in ('site/catalog-v3.json', 'site/catalog-v4.json', 'tamilanstore/src/main/assets/catalog.json'):
            apps = json.loads((ROOT/path).read_text())['apps']
            entry, = [a for a in apps if a['id']=='weeklymoney']
            self.assertEqual((entry['version'], entry['url'], entry['sha256'], entry['size']), ('1.7', URL, SHA, receipt['bytes']))
        site = (ROOT/'site/index.html').read_text()
        pages = (ROOT/'.github/workflows/pages.yml').read_text()
        self.assertIn(URL, site)
        self.assertIn(SHA, site)
        self.assertIn(('("weekly-money-v1.7", "Weekly-Money-v1.7.apk", 7499595, "' + SHA + '")'), pages)
        self.assertIn('Weekly-Money-v1.7.zip', pages)
        self.assertIn('weekly-money-v1.7', (ROOT/'site/weekly-money-guide.html').read_text())

    def test_about_version_is_from_the_tested_apk_not_hardcoded(self):
        app = (ROOT/'weeklymoney/app/src/main/java/com/universalrp/weeklymoney/MainActivity.kt').read_text()
        gradle = (ROOT/'weeklymoney/app/build.gradle.kts').read_text()
        self.assertIn('Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})', app)
        self.assertIn('versionName = "1.7"', gradle)
        self.assertIn('versionCode = 19', gradle)

if __name__ == '__main__':
    unittest.main()
