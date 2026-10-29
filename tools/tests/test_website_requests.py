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

if __name__ == '__main__':
    unittest.main()
