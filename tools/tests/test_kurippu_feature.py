"""The new release feature uses the approved public APK and owner-supplied earlier screens."""
import json
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]

class KurippuFeatureTest(unittest.TestCase):
    def test_feature_links_exact_release_and_explains_screenshot_version_and_data_risk(self):
        page = (ROOT / 'site/index.html').read_text()
        approved = json.loads((ROOT / 'docs/kurippu/approved-1.0.json').read_text())
        self.assertIn('https://github.com/universalrp2003/RP-Android-Apps/releases/tag/kurippu-v1.0', page)
        self.assertIn(approved['apk_sha256'], page)
        self.assertIn('owner supplied these 0.6 screenshots', page.lower())
        self.assertIn('No backup or sync', page)
        self.assertIn("public release linked below is 1.0", page)
        for name in ("owner-1.1-calculator.jpg", "owner-1.1-page-menu.jpg"):
            self.assertIn("images/kurippu/" + name, page)
            self.assertGreater((ROOT / "site/images/kurippu" / name).stat().st_size, 0)
        for index in range(1, 7):
            path = f'images/kurippu/owner-0.6-{index}.jpg'
            self.assertIn(path, page)
            self.assertGreater((ROOT/'site'/path).stat().st_size, 0)
