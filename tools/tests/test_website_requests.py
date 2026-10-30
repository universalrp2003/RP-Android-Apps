from pathlib import Path
import json
import re
import sys
import tempfile
import unittest
from html.parser import HTMLParser
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import verify_apps_localization as localization  # noqa: E402

class PageParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.elements = []
    def handle_starttag(self, tag, attrs):
        self.elements.append((tag, dict(attrs)))

class WebsiteRequestsTest(unittest.TestCase):
    def setUp(self):
        self.source = (ROOT / 'site/index.html').read_text()
        self.parser = PageParser()
        self.parser.feed(self.source)

    def test_request_form_routes_to_public_github_issue_draft(self):
        form = next(a for tag, a in self.parser.elements if tag == 'form' and a.get('id') == 'app-request-form')
        self.assertEqual(form['action'], 'https://github.com/universalrp2003/RP-Android-Apps/issues/new')
        self.assertEqual(form['method'], 'get')
        fields = {a.get('name'): a for tag, a in self.parser.elements if tag in ('input', 'textarea')}
        for name in ('title', 'body'):
            self.assertIn('required', fields[name])
            self.assertIn('maxlength', fields[name])
        self.assertIn('Sign in to GitHub', self.source)
        self.assertIn('Requests and replies are public', self.source)
        self.assertIn('does not save or send the form itself', self.source)

    def test_social_metadata_covers_all_apps_and_favicon_exists(self):
        tags = {a.get('name', a.get('property')): a for tag, a in self.parser.elements if tag == 'meta'}
        description = tags['twitter:description']['content']
        keywords = tags['keywords']['content']
        for app_name in ('DailyVault', 'Tamilan Store'):
            self.assertIn(app_name, description)
            self.assertIn(app_name, keywords)
        for tamil_name in ('கைபேசி காவலன்', 'தமிழன் வானொலி', 'இசை மட்டும் வேண்டும்'):
            self.assertIn(tamil_name, description)
        favicon = next(a for tag, a in self.parser.elements if tag == 'link' and a.get('rel') == 'icon')
        self.assertTrue((ROOT / 'site' / favicon['href']).is_file())

    def test_evidence_is_collapsed_after_app_cards(self):
        details = next(a for tag, a in self.parser.elements if tag == 'details' and a.get('class') == 'trust release-status')
        self.assertNotIn('open', details)
        self.assertGreater(self.source.index('class="trust release-status"'), self.source.index('id="request-app"'))
        self.assertGreater(self.source.index('id="request-app"'), self.source.index('<!-- Voice Notes -->'))

    def test_suite_counts_hashes_and_sizes_match_across_header_table_and_cards(self):
        import re
        sister_source = (ROOT / 'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        expected_total = len(re.findall(r'^\s+SisterApp\(', sister_source, re.M))
        expected_optional = 2
        expected_non_store = expected_total - expected_optional
        self.assertEqual(expected_total, 14)
        self.assertEqual(expected_non_store, 12)

        self.assertIn(f'{expected_total} apps ({expected_non_store} non-Store + {expected_optional} optional: Tamilan Store &amp; DailyVault)', self.source)
        self.assertIn(f'Fourteen lightweight, privacy-respecting Android apps ({expected_non_store} non-Store apps + {expected_optional} optional: Tamilan Store and DailyVault)', self.source)
        self.assertNotIn('13 non-Store + optional Tamilan Store', self.source)
        self.assertNotIn('13 non-Store apps + optional Tamilan Store', self.source)

        en_counts = [int(n) for n in re.findall(r'\ball (\d+) RP Ideas apps\b', self.source)]
        ta_counts = [int(n) for n in re.findall(r'அனைத்து (\d+) RP Ideas செயலிகள்', self.source)]
        hyphen_counts = [int(n) for n in re.findall(r'\b(\d+)-app\b', self.source)]
        self.assertEqual(en_counts, [expected_total, expected_total])
        self.assertEqual(ta_counts, [expected_total, expected_total])
        self.assertTrue(all(n == expected_total for n in hyphen_counts), hyphen_counts)
        self.assertNotIn('blob/arena/', self.source)
        self.assertNotIn('Android8.0+', self.source)

    def test_json_ld_table_and_cards_cover_the_same_fourteen_releases(self):
        data = json.loads(re.search(
            r'<script type="application/ld\+json">(.*?)</script>', self.source, re.S
        ).group(1))
        items = data['mainEntity']['itemListElement']
        self.assertEqual([item['position'] for item in items], list(range(1, 15)))
        ld_assets = {item['item']['downloadUrl'].rsplit('/', 1)[-1] for item in items}
        ld_tags = {item['item']['url'].rsplit('/', 1)[-1] for item in items}
        table = re.findall(
            r'<td class="ldl"><a class="btn btn-primary" data-apk="apks/([A-Za-z0-9._-]+\.apk)"'
            r'[^>]*href="https://github\.com/universalrp2003/RP-Android-Apps/releases/download/([a-z0-9.-]+)/\1"',
            self.source,
        )
        cards = re.findall(
            r'<article class="card">.*?data-apk="apks/([A-Za-z0-9._-]+\.apk)"'
            r'[^>]*href="https://github\.com/universalrp2003/RP-Android-Apps/releases/download/([a-z0-9.-]+)/\1"',
            self.source, re.S,
        )
        self.assertEqual(len(items), 14)
        self.assertEqual(len(table), 14)
        self.assertEqual(len(cards), 14)
        self.assertEqual({asset for asset, _ in table}, ld_assets)
        self.assertEqual({asset for asset, _ in cards}, ld_assets)
        self.assertEqual({tag for _, tag in table}, ld_tags)
        self.assertEqual({tag for _, tag in cards}, ld_tags)
        self.assertIn('Tamilan-Store-v0.6.apk', ld_assets)
        self.assertIn('DailyVault-v0.4.apk', ld_assets)
        optional_tags = {'tamilan-store-v0.6', 'daily-vault-v0.4'}
        self.assertTrue(optional_tags.issubset(ld_tags))
        self.assertEqual(len(optional_tags), 2)
        self.assertEqual(len(ld_tags - optional_tags), 12)
        self.assertEqual(localization.verify_site_versions_match_downloads(), (42, 14))

    def assert_site_rejected(self, source):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'site').mkdir()
            (root / 'site/index.html').write_text(source)
            sister = root / 'shared/src/main/java/com/universalrp/updates/SisterApps.kt'
            sister.parent.mkdir(parents=True)
            sister.write_text((ROOT / 'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text())
            with mock.patch.object(localization, 'ROOT', root):
                with self.assertRaises(ValueError):
                    localization.verify_site_versions_match_downloads()

    def test_owner_promise_ticker_is_prominent_bilingual_and_pausable(self):
        tickers = [a for tag, a in self.parser.elements if tag == 'section' and 'data-trust-ticker' in a]
        self.assertEqual(len(tickers), 1)
        first = self.source.index('<section class="trust-ticker" data-trust-ticker')
        badge = self.source.index('<span class="badge">')
        migration = self.source.index('<section class="migration-notice"')
        self.assertLess(first, badge)
        self.assertLess(first, migration)
        self.assertNotIn('class="trust-ticker trust-ticker--hero"', self.source)

        english = (
            '100% open source — every line is public on GitHub for anyone to audit.',
            'No ads · no tracking · no analytics · no accounts — ever.',
        )
        for text in english:
            self.assertGreaterEqual(self.source.count(text), 2)
        self.assertGreaterEqual(self.source.count('lang="ta"'), 4)
        self.assertIn('100% திறந்த மூலம்', self.source)
        self.assertIn('விளம்பரம் இல்லை · கண்காணிப்பு இல்லை · பயன்பாட்டுப் பகுப்பாய்வு இல்லை · பயனர் கணக்கு தேவையில்லை', self.source)

        pause_buttons = [a for tag, a in self.parser.elements if tag == 'button' and 'data-trust-pause' in a]
        self.assertEqual(len(pause_buttons), 1)
        for attrs in pause_buttons:
            self.assertEqual(attrs.get('type'), 'button')
            self.assertEqual(attrs.get('aria-pressed'), 'false')
        self.assertIn('prefers-reduced-motion: reduce', self.source)
        self.assertIn('.trust-ticker.is-paused .trust-ticker__track', self.source)
        self.assertIn("ticker.classList.toggle('is-paused')", self.source)
        self.assertIn('.trust-promise--source { color: #ffd166; }', self.source)
        self.assertIn('.trust-promise--privacy { color: #86efac; }', self.source)
        self.assertIn('.trust-promise--ta { color: #f5d0fe;', self.source)
        self.assertNotIn('<marquee', self.source)

    def test_parity_gate_rejects_missing_apps_duplicates_and_stale_metadata(self):
        missing_json_ld = self.source.replace('"position": 14,', '"position": 13,')
        self.assert_site_rejected(missing_json_ld)
        duplicate_card = self.source.replace('data-apk="apks/DailyVault-v0.4.apk"',
                                             'data-apk="apks/Tamilan-Store-v0.6.apk"', 1)
        self.assert_site_rejected(duplicate_card)
        stale_sha = self.source.replace(
            'SHA-256: 95a050510786f1e4a34746cb0b3dcf5d3a3a032ec61052a94745d8f7719941ac',
            'SHA-256: ' + '0' * 64,
        )
        self.assert_site_rejected(stale_sha)
        stale_size = self.source.replace('<div class="mini">12.2 MB', '<div class="mini">12.1 MB')
        self.assert_site_rejected(stale_size)
        stale_tamil_count = self.source.replace('அனைத்து 14 RP Ideas செயலிகள்',
                                                'அனைத்து 7 RP Ideas செயலிகள்', 1)
        self.assert_site_rejected(stale_tamil_count)
        stale_header = self.source.replace(
            '14 apps (12 non-Store + 2 optional: Tamilan Store &amp; DailyVault)',
            '14 apps (13 non-Store + optional Tamilan Store)',
        )
        self.assert_site_rejected(stale_header)
        stale_intro = self.source.replace(
            '(12 non-Store apps + 2 optional: Tamilan Store and DailyVault)',
            '(13 non-Store apps + optional Tamilan Store)',
        )
        self.assert_site_rejected(stale_intro)
        missing_optional_name = self.source.replace(
            '2 optional: Tamilan Store &amp; DailyVault',
            '2 optional: Tamilan Store',
        )
        self.assert_site_rejected(missing_optional_name)
        temporary_link = self.source.replace(
            'blob/main/docs/2026-10-27-pulseeq-device-review.md',
            'blob/arena/example/docs/2026-10-27-pulseeq-device-review.md',
        )
        self.assert_site_rejected(temporary_link)


if __name__ == '__main__':
    unittest.main()
