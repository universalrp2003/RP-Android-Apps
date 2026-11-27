"""The new release feature uses the approved public APK and owner-supplied earlier screens."""
import json
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]

class KurippuFeatureTest(unittest.TestCase):
    def test_feature_links_exact_release_and_explains_screenshot_version_and_data_risk(self):
        page = (ROOT / 'site/index.html').read_text()
        approved = json.loads((ROOT / 'docs/kurippu/approved-1.3.json').read_text())
        self.assertIn('https://github.com/universalrp2003/RP-Android-Apps/releases/tag/kurippu-v1.3', page)
        self.assertIn(approved['apk_sha256'], page)
        self.assertIn('owner supplied these 0.6 screenshots', page.lower())
        self.assertIn('No backup or sync', page)
        self.assertIn("public 1.3 release saves the last six", page)
        for name in ("owner-1.1-calculator.jpg", "owner-1.1-page-menu.jpg"):
            self.assertIn("images/kurippu/" + name, page)
            self.assertGreater((ROOT / "site/images/kurippu" / name).stat().st_size, 0)
        for index in range(1, 7):
            path = f'images/kurippu/owner-0.6-{index}.jpg'
            self.assertIn(path, page)
            self.assertGreater((ROOT/'site'/path).stat().st_size, 0)

    def test_kurippu_is_a_normal_selectable_release_with_exact_mirror_identity(self):
        import re
        page = (ROOT / 'site/index.html').read_text()
        controls = (ROOT / 'site/site-controls.js').read_text()
        workflow = (ROOT / '.github/workflows/pages.yml').read_text()
        pin = json.loads((ROOT / 'docs/kurippu/approved-1.3.json').read_text())
        table = page.split('<tbody>', 1)[1].split('</tbody>', 1)[0]
        card = re.search(r'<article class="card"[^>]*>.*?<h2 id="app-kurippu".*?</article>', page, re.S).group()
        row = re.search(r'<tr>\s*<td class="lname"><a href="#app-kurippu">.*?</tr>', table, re.S).group()
        for block in (card, row):
            self.assertIn('kurippu-v1.3/Kurippu-v1.3.apk', block)
            self.assertIn(pin['apk_sha256'], block)
            self.assertIn('2026-11-26T15:10:11Z', block)
        self.assertIn("'kurippu': 'productivity'", controls)
        self.assertIn("document.querySelectorAll('article.card')", controls)
        self.assertIn("jump.appendChild(option)", controls)
        self.assertIn(f'("kurippu-v1.3", "Kurippu-v1.3.apk", {pin["bytes"]}, "{pin["apk_sha256"]}")', workflow)
        self.assertIn('href="apks/Kurippu-v1.3.zip"', card)
