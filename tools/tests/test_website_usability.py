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
        self.assertEqual(dates.check(self.source, self.evidence), 16)
        for value in self.evidence['releases'].values():
            self.assertIsNotNone(datetime.fromisoformat(value.replace('Z', '+00:00')).tzinfo)

    def test_every_card_states_its_own_release_date(self):
        dates_on_cards = [(m.group(1), m.group(2)) for m in re.finditer(
            r'<time class="card-date" datetime="([^"]+)" data-release-tag="([^"]+)">', self.source)]
        self.assertEqual(len(dates_on_cards), 16)
        for value, tag in dates_on_cards:
            self.assertEqual(self.evidence['releases'][tag], value)
        # and the visible label is a readable date, not a raw timestamp
        labels = re.findall(r'<time class="card-date"[^>]*>([^<]+)</time>', self.source)
        for label in labels:
            self.assertRegex(label, r'^\d{2} [A-Z][a-z]{2} \d{4}$')

    def test_changed_tag_and_stale_date_are_rejected(self):
        stamp = self.evidence['releases']['live-guard-v3.00']
        for stale in [self.source.replace('datetime="' + stamp + '"', 'datetime="2025-01-01T01:00:00Z"', 1),
                      self.source.replace('>30 Oct 2026</time>', '>31 Oct 2026</time>', 1),
                      self.source.replace('data-release-tag="live-guard-v3.00"', 'data-release-tag="live-guard-v0.0"', 1)]:
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
        # Corrupt the table row's tag only; the download link still names the release, so
        # refresh can recover the date from real evidence.
        live = self.source.index('data-release-tag="live-guard-v3.00"')
        row_start = self.source.rindex('<tr>', 0, live)
        row_end = self.source.index('</tr>', live) + len('</tr>')
        row = self.source[row_start:row_end]
        stale = (self.source[:row_start]
                 + row.replace('data-release-tag="live-guard-v3.00"', 'data-release-tag="old-version"')
                 + self.source[row_end:])
        self.assertNotEqual(stale, self.source)
        source, evidence = dates.refresh(stale, lambda tag: {'tag_name': tag, 'published_at': self.evidence['releases'][tag]})
        self.assertEqual(source, self.source)
        self.assertEqual(evidence, self.evidence)

    def test_a_card_naming_an_unknown_release_is_rejected(self):
        """A card's own date cannot be invented: it must come from a pinned release."""
        stale = self.source.replace(
            '<time class="card-date" datetime="2026-11-11T12:00:01Z" data-release-tag="tvguard-v1.5.1">',
            '<time class="card-date" datetime="2026-11-11T12:00:01Z" data-release-tag="not-a-release">',
            1,
        )
        self.assertNotEqual(stale, self.source)
        with self.assertRaises(ValueError):
            dates.refresh(stale, lambda tag: {'tag_name': tag, 'published_at': self.evidence['releases'][tag]})

    def test_unpublished_release_cannot_supply_a_date(self):
        for release in [{'tag_name': 'not-the-tag', 'published_at': '2026-10-30T00:00:00Z'},
                        {'draft': True}, {'published_at': None}]:
            with self.assertRaises(ValueError):
                dates.refresh(self.source, lambda tag: release)

    def test_all_fifteen_static_app_anchors_and_download_names_exist(self):
        cards = re.findall(r'<article class="card"[^>]*>.*?</article>', self.source, re.S)
        anchors = set()
        for card in cards:
            anchor = re.search(r'<h2 id="(app-[a-z-]+)" tabindex="-1">', card).group(1)
            self.assertNotIn(anchor, anchors)
            anchors.add(anchor)
            self.assertIn(f'href="#{anchor}"', self.source)
            self.assertRegex(card, r'data-apk="[^"]+"[^>]*aria-label="Download APK · [^"]+"')
        self.assertEqual(len(anchors), 16)

    def test_card_order_is_newest_release_first(self):
        """The page promises the newest release first, so a stale order must fail the gate."""
        site = self.source
        import sys
        from pathlib import Path as _P
        sys.path.insert(0, str(_P(__file__).resolve().parents[1]))
        import verify_apps_localization as localization
        regular = re.search(
            r'aria-label="15 regular Android applications">(.*?)</section>', site, re.S
        )[1]
        ids = re.findall(r'<h2 id="(app-[^"]+)"', regular)
        self.assertEqual(len(ids), 15)
        localization.verify_newest_first(site, ids)
        self.assertEqual(ids[0], 'app-system-app-disabler')
        self.assertEqual(ids[1], 'app-live-guard')
        self.assertEqual(ids[2], 'app-tv-guard')

        # Moving the oldest card to the front must be rejected.
        oldest = site.index('        <!-- Secret Message -->')
        end = site.index('</article>', oldest) + len('</article>')
        block = site[oldest:end]
        shuffled = site[:oldest] + site[end:]
        first = shuffled.index('        <!-- System App Disabler')
        shuffled = shuffled[:first] + block + '\n\n' + shuffled[first:]
        shuffled_ids = re.findall(
            r'<h2 id="(app-[^"]+)"',
            re.search(r'aria-label="15 regular Android applications">(.*?)</section>', shuffled, re.S)[1],
        )
        self.assertEqual(shuffled_ids[0], 'app-secret-message')
        with self.assertRaises(ValueError):
            localization.verify_newest_first(shuffled, shuffled_ids)
        # And moving a middle card above the newest one must be rejected too.
        pristine = site
        middle = pristine.index('        <!-- Live Guard -->')
        end = pristine.index('</article>', middle) + len('</article>')
        block = pristine[middle:end]
        reordered = pristine[:middle] + pristine[end:]
        top = reordered.index('        <!-- System App Disabler')
        reordered = reordered[:top] + block + '\n\n' + reordered[top:]
        ids2 = re.findall(
            r'<h2 id="(app-[^"]+)"',
            re.search(r'aria-label="15 regular Android applications">(.*?)</section>', reordered, re.S)[1],
        )
        self.assertEqual(ids2[0], 'app-live-guard')
        with self.assertRaises(ValueError):
            localization.verify_newest_first(reordered, ids2)

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
        self.assertLess(self.source.index('</style>'), self.source.index('href="site-controls.css'))

    def test_current_release_disclosures_are_not_hidden_as_technical_history(self):
        self.assertEqual(self.source.count('class="release-brief"'), 16)
        self.assertEqual(self.source.count('class="testing-status"'), 16)
        self.assertIn('Earlier feature history', self.source)
        self.assertIn('this exact APK was not phone-tested before publication', self.source)
        self.assertIn('same-key', self.source)
        self.assertIn('not for an ordinary same-key update', self.source)
        self.assertIn('Copying a hash does not verify a downloaded file.', self.source)
        self.assertIn('not a safety approval', self.source)

    def test_social_preview_is_local_small_and_1200_by_630(self):
        image = (ROOT / 'site/images/social-preview-v16.png').read_bytes()
        self.assertEqual(image[:8], b'\x89PNG\r\n\x1a\n')
        self.assertEqual(struct.unpack('>II', image[16:24]), (1200, 630))
        self.assertLess(len(image), 200_000)
        self.assertIn('RP-Android-Apps/images/social-preview-v16.png', self.source)
        self.assertIn(b'16 free Android apps; 15 regular + 1 optional app', image)
        self.assertEqual(self.source.count('RP-Android-Apps/images/social-preview-v16.png'), 2)
        self.assertEqual(len(re.findall(r'<article class="card"', self.source)), 16)
        self.assertIn('aria-label="15 regular Android applications"', self.source)


if __name__ == '__main__':
    unittest.main()
