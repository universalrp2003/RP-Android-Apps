"""The GitHub front page is README.md: its release table must list each app's current public release,
never an older one. Derived from SisterApps.kt so a bump that forgets the README fails here."""
import re
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
README = (ROOT/'README.md').read_text(encoding='utf-8')
SISTER = (ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text(encoding='utf-8')
BASE = 'https://github.com/universalrp2003/RP-Android-Apps/releases'

class ReadmeTableMatchesPublicReleasesTest(unittest.TestCase):
    def sister_apps(self):
        # publishedVersion is optional in the data class (it defaults to version); secretmessage relies on that.
        pattern = re.compile(r'SisterApp\(\s*id = "([^"]+)",(?:(?!SisterApp\().)*?tagPrefix = "([^"]+)",(?:(?!SisterApp\().)*?version = "([^"]+)"(?:,\s*publishedVersion = "([^"]+)")?', re.S)
        return {app: (prefix, published or version) for app, prefix, version, published in pattern.findall(SISTER)}

    def test_every_regular_app_row_names_its_current_public_release(self):
        row = re.compile(r'^\| \*{0,2}(?P<title>[^|]+?)\*{0,2}\]?[^|]*\| \[(?P<version>[\d.]+) / code (?P<code>\d+)\]\(' + re.escape(BASE) + r'/tag/(?P<tag>[a-z0-9.-]+)\) \| \[`(?P<asset>[^`]+)`\]\((?P<url>[^)]+)\) \|$', re.M)
        rows = row.findall(README)
        self.assertEqual(17, len(rows), 'the README table lists the seventeen regular apps')
        apps = self.sister_apps()
        self.assertEqual(18, len(apps))
        seen = set()
        for title, version, code, tag, asset, url in rows:
            prefix, published = tag.rsplit('-v', 1)[0], tag.rsplit('-v', 1)[1]
            match = [(app, v) for app, (p, v) in apps.items() if p == prefix]
            self.assertEqual(1, len(match), f'{tag}: no unique SisterApps entry with prefix {prefix}')
            app, current = match[0]
            self.assertNotEqual('tamilanstore', app, 'the Store is a bullet above the table, not a row')
            self.assertEqual(current, published, f'{title}: README lists {tag} but {app} is published at {current}')
            self.assertEqual(current, version, f'{title}: table version {version} differs from tag {tag}')
            self.assertEqual(f'{BASE}/download/{tag}/{asset}', url, title)
            self.assertTrue(asset.endswith('.apk'), asset)
            seen.add(prefix)
        self.assertEqual({p for app, (p, v) in apps.items() if app != 'tamilanstore'}, seen)

    def test_store_bullet_names_the_current_store_release(self):
        prefix, published = self.sister_apps()['tamilanstore']
        m = re.search(r'Tamilan Store([\d.]+)/code(\d+) — current stable APK\]\((' + re.escape(BASE) + r'/download/[^)]+)\)', README)
        self.assertIsNotNone(m, 'the Store bullet with its current stable APK link')
        self.assertEqual(published, m.group(1))
        self.assertEqual(f'{BASE}/download/{prefix}-v{published}/Tamilan-Store-v{published}.apk', m.group(3))

if __name__ == '__main__':
    unittest.main()
