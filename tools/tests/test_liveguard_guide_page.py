import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GUIDE = ROOT / 'site/live-guard-guide.html'


class LiveGuardGuidePage(unittest.TestCase):
    """The published Live Guard guide must describe the release that is actually live."""

    def setUp(self):
        self.guide = GUIDE.read_text()
        self.site = (ROOT / 'site/index.html').read_text()
        self.receipt = json.loads((ROOT / 'docs/live-guard-3.00-publication-receipt.json').read_text())

    def test_guide_exists_and_is_linked_from_the_live_guard_card(self):
        self.assertIn('href="live-guard-guide.html"', self.site)
        card = self.site.split('<h2 id="app-live-guard"', 1)[1].split('</article>', 1)[0]
        self.assertIn('live-guard-guide.html', card)

    def test_guide_downloads_the_published_bytes(self):
        self.assertEqual(self.receipt['apk_sha256'],
                         '286f172054f0a9b10a16d673f49acff1a3bac3d8b3686c9ddf65c27b2b2a5940')
        self.assertIn(self.receipt['apk_sha256'], self.guide)
        self.assertIn(f"{self.receipt['bytes']:,} bytes", self.guide)
        self.assertIn(self.receipt['url'], self.guide)

    def test_guide_is_bilingual_and_names_the_real_screens(self):
        # English body plus a Tamil section that is not a stub.
        tamil = self.guide.split('<section id="tamil"', 1)[1]
        tamil_body = tamil.split('</section>', 1)[0]
        self.assertGreater(len(tamil_body), 1500)
        for topic in ('எனது திட்டம்', 'மிதப்புப் பட்டி', '4G, 5G', 'சுத்தம்', 'தனியுரிமை'):
            self.assertIn(topic, tamil_body)

    def test_every_documented_screenshot_exists(self):
        for src in re.findall(r'src="(images/liveguard-v3\.00/[^"]+)"', self.guide):
            self.assertTrue((ROOT / 'site' / src).is_file(), src)
        self.assertEqual(len(re.findall(r'src="images/liveguard-v3\.00/', self.guide)), 5)

    def test_guide_does_not_promise_what_the_app_does_not_do(self):
        # Standing owner rules: no AI claims, Accessibility stays gone, no red-removal.
        self.assertNotIn('Accessibility service is used', self.guide)
        self.assertIn('Accessibility service was dropped in v1.3 and stays gone', self.guide)
        # The red LED is a required feature and must be described as one.
        self.assertIn('turns red on a bad connection', self.guide)

    def test_phone_test_is_recorded_honestly(self):
        self.assertTrue(self.receipt['physical_phone_tested'])
        self.assertFalse(self.receipt['installed_phone_hash_matched'])
        self.assertIn('no on-device SHA-256 comparison', self.receipt['note_on_installed_hash'])


if __name__ == '__main__':
    unittest.main()
