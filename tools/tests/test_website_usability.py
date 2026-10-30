"""Offline regression contracts for progressive website features and date provenance."""
from datetime import datetime
import importlib.util
import json
from pathlib import Path
import re
import struct
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('website_dates', ROOT / 'tools/update_website_release_dates.py')
dates = importlib.util.module_from_spec(spec)
spec.loader.exec_module(dates)


class WebsiteUsabilityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = (ROOT / 'site/index.html').read_text()
        cls.evidence = json.loads((ROOT / 'site/release-dates.json').read_text())
        cls.script = (ROOT / 'site/site-controls.js').read_text()
        cls.css = (ROOT / 'site/site-controls.css').read_text()

    def test_every_release_has_a_matching_real_timestamp(self):
        self.assertEqual(dates.check(self.source, self.evidence), 14)
        for value in self.evidence['releases'].values():
            self.assertIsNotNone(datetime.fromisoformat(value.replace('Z', '+00:00')).tzinfo)

    def test_changed_tag_and_stale_date_are_rejected(self):
        stamp = self.evidence['releases']['live-guard-v2.79']
        for stale in [self.source.replace('datetime="' + stamp + '"', 'datetime="2025-01-01T01:00:00Z"', 1),
                      self.source.replace('>30 Oct 2026</time>', '>31 Oct 2026</time>', 1),
                      self.source.replace('data-release-tag="live-guard-v2.79"', 'data-release-tag="live-guard-v0.0"', 1)]:
            with self.assertRaises(ValueError):
                dates.check(stale, self.evidence)

    def test_timestamp_parser_rejects_local_dates_and_impossible_dates(self):
        for value in ['2026-10-30', '2026-10-30T00:00:00+05:30', '2026-02-30T00:00:00Z']:
            with self.assertRaises(ValueError):
                dates.date_label(value)

    def test_refresh_uses_only_github_publication_values(self):
        def load(tag):
            return {'tag_name': tag, 'published_at': self.evidence['releases'][tag], 'draft': False}
        source, evidence = dates.refresh(self.source, load)
        self.assertEqual(source, self.source)
        self.assertEqual(evidence, self.evidence)

    def test_refresh_repairs_date_tag_after_a_pinned_release_changes(self):
        stale = self.source.replace('data-release-tag="live-guard-v2.79"', 'data-release-tag="old-version"')
        source, evidence = dates.refresh(stale, lambda tag: {'tag_name': tag, 'published_at': self.evidence['releases'][tag]})
        self.assertEqual(source, self.source)
        self.assertEqual(evidence, self.evidence)

    def test_unpublished_release_cannot_supply_a_date(self):
        for release in [{'tag_name': 'not-the-tag', 'published_at': '2026-10-30T00:00:00Z'},
                        {'draft': True}, {'published_at': None}]:
            with self.assertRaises(ValueError):
                dates.refresh(self.source, lambda tag: release)

    def test_all_fourteen_static_app_anchors_and_download_names_exist(self):
        cards = re.findall(r'<article class="card">.*?</article>', self.source, re.S)
        anchors = set()
        for card in cards:
            anchor = re.search(r'<h2 id="(app-[a-z-]+)" tabindex="-1">', card).group(1)
            self.assertNotIn(anchor, anchors)
            anchors.add(anchor)
            self.assertIn(f'href="#{anchor}"', self.source)
            self.assertRegex(card, r'data-apk="[^"]+"[^>]*aria-label="Download APK · [^"]+"')
        self.assertEqual(len(anchors), 14)

    def test_features_are_progressive_local_and_accessibly_labelled(self):
        self.assertIn('<option value="both">', self.source)
        self.assertIn('aria-label="App categories"', self.source)
        self.assertIn('role="status" aria-live="polite"', self.source)
        self.assertIn('navigator.clipboard.writeText(hash)', self.script)
        self.assertIn('Automatic copy unavailable.', self.script)
        self.assertIn("feedback.setAttribute('role', 'status')", self.script)
        self.assertIn('[hidden] { display: none !important; }', self.css)
        self.assertIn(':focus-visible', self.css)
        self.assertNotIn('fetch(', self.script)
        self.assertNotIn('https://', self.script)
        self.assertLess(self.source.index('</style>'), self.source.index('href="site-controls.css"'))

    def test_current_release_disclosures_are_not_hidden_as_technical_history(self):
        self.assertEqual(self.source.count('class="release-brief"'), 14)
        self.assertEqual(self.source.count('class="testing-status"'), 14)
        self.assertIn('Earlier feature history', self.source)
        self.assertIn('this exact APK was not phone-tested before publication', self.source)
        self.assertIn('same-key', self.source)
        self.assertIn('do not uninstall it', self.source)
        self.assertIn('Copying a hash does not verify a downloaded file.', self.source)
        self.assertIn('not security approval', self.source)

    def test_social_preview_is_local_small_and_1200_by_630(self):
        image = (ROOT / 'site/images/social-preview.png').read_bytes()
        self.assertEqual(image[:8], b'\x89PNG\r\n\x1a\n')
        self.assertEqual(struct.unpack('>II', image[16:24]), (1200, 630))
        self.assertLess(len(image), 200_000)
        self.assertIn('RP-Android-Apps/images/social-preview.png', self.source)


if __name__ == '__main__':
    unittest.main()
