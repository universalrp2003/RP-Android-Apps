"""The mobile size beside each APK must match the signed release mirror pin."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]


class WebsiteApkSizes(unittest.TestCase):
    def test_each_release_size_matches_mirror_bytes(self):
        html = (ROOT / 'site/index.html').read_text()
        pages = (ROOT / '.github/workflows/pages.yml').read_text()
        pins = {name: int(size) for _, name, size in re.findall(
            r'\("([^\"]+)", "([^\"]+\.apk)", (\d+), "[a-f0-9]{64}"\)', pages)}
        section = html.split('<section class="latest"', 1)[1].split('</table>', 1)[0]
        rows = re.findall(r'<tr>(.*?)</tr>', section, re.S)
        seen = set()
        for row in rows:
            name = re.search(r'data-name="([^\"]+\.apk)"', row)
            if not name:
                continue
            name = name.group(1)
            self.assertIn(name, pins)
            label = f'{pins[name] / 1_000_000:.1f} MB'
            self.assertIn(f'<td class="lsize">{label}</td>', row)
            self.assertIn(f'<span class="apk-size-mobile" aria-label="APK size: {label}">{label}</span>', row)
            seen.add(name)
        self.assertEqual(seen, set(pins))
        self.assertEqual(len(seen), 16)

    def test_version_header_hides_with_its_column(self):
        html = (ROOT / 'site/index.html').read_text()
        self.assertIn('<th class="lver">Version</th>', html)
        self.assertIn('.latest .apk-size-mobile { display: block;', html)
