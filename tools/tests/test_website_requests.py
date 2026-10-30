from pathlib import Path
import unittest
from html.parser import HTMLParser

ROOT = Path(__file__).resolve().parents[2]

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
        expected_non_store = expected_total - 1
        self.assertEqual(expected_total, 14)

        self.assertIn(f'{expected_total} apps ({expected_non_store} non-Store + optional Tamilan Store)', self.source)
        self.assertIn(f'Fourteen lightweight, privacy-respecting Android apps ({expected_non_store} non-Store apps + optional Tamilan Store)', self.source)

        en_counts = [int(n) for n in re.findall(r'\ball (\d+) RP Ideas apps\b', self.source)]
        ta_counts = [int(n) for n in re.findall(r'அனைத்து (\d+) RP Ideas செயலிகள்', self.source)]
        hyphen_counts = [int(n) for n in re.findall(r'\b(\d+)-app\b', self.source)]
        self.assertEqual(en_counts, [expected_total, expected_total])
        self.assertEqual(ta_counts, [expected_total, expected_total])
        self.assertTrue(all(n == expected_total for n in hyphen_counts), hyphen_counts)
        self.assertNotIn('blob/arena/', self.source)
        self.assertNotIn('Android8.0+', self.source)


if __name__ == '__main__':
    unittest.main()
