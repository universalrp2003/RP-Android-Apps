"""Static contracts for the GitHub Pages APK/ZIP mirror set.

The Pages artifact had grown to ~2 GB and the deploy step warned that it exceeded the 1 GB limit.
The mirror now keeps only what the site and the live verifier actually read:

* the current release of every app (the site's APK button fallback, its .ZIP button and the
  structured-data downloadUrl all point at these files), and
* the historical builds that ``tools/verify_live_three_releases.py`` byte-checks after every
  deploy.

Everything else stays on its GitHub release page, which is never deleted, so no download is lost
- it simply is not mirrored onto the website a second time.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
PAGES = (ROOT / '.github/workflows/pages.yml').read_text()
VERIFIER = (ROOT / 'tools/verify_live_three_releases.py').read_text()
SITE = (ROOT / 'site/index.html').read_text()

MIRROR = re.findall(r'\("([^"]+)", "([^"]+)", (\d+), "([0-9a-f]{64})"\)', PAGES)
# Everything the live verifier pins by asset name; those historical mirrors must stay readable.
VERIFIED_ASSETS = set(re.findall(r"\('([\w.\-]+\.apk)',\s*(?:json\.loads|store|health)", VERIFIER))


class SiteMirrorBudgetTest(unittest.TestCase):
    def test_deployment_window_leaves_time_for_strict_live_checks(self):
        deploy = PAGES.split('\n  deploy:\n', 1)[1]
        job_minutes = int(re.search(r'timeout-minutes: (\d+)', deploy).group(1))
        action_ms = int(re.search(r'uses: actions/deploy-pages@v5\s+with:\s+timeout: (\d+)', deploy).group(1))
        self.assertEqual(action_ms, 600_000)
        self.assertGreaterEqual(job_minutes * 60_000 - action_ms, 600_000)
        self.assertIn('run: python3 tools/verify_live_three_releases.py', deploy)
        self.assertNotIn('continue-on-error:', deploy)

    def test_mirror_set_is_current_releases_plus_byte_checked_history(self):
        self.assertGreaterEqual(len(MIRROR), 16)
        self.assertEqual(len(MIRROR), len({tag for tag, _, _, _ in MIRROR}), 'duplicate mirrored tag')
        self.assertEqual(len(MIRROR), len({asset for _, asset, _, _ in MIRROR}), 'duplicate mirrored asset')
        current = set(re.findall(r'data-release-tag="([^"]+)"', SITE))
        self.assertEqual(len(current), 16, 'the site no longer names one release per app')
        self.assertGreaterEqual(len(VERIFIED_ASSETS), 10, 'the live verifier pins fewer assets than expected')
        mirrored_tags = {tag for tag, _, _, _ in MIRROR}
        for tag, asset, size, digest in MIRROR:
            self.assertGreater(int(size), 0, tag)
            self.assertRegex(digest, r'^[0-9a-f]{64}$', tag)
            self.assertTrue(tag in current or asset in VERIFIED_ASSETS,
                            f'{tag} is neither the current release of an app nor a byte-checked historical pin')
        # Every app's current download must be mirrored, otherwise its APK/.ZIP buttons break.
        self.assertEqual(current, mirrored_tags)
        self.assertIn("if name not in current_names:", VERIFIER)
        self.assertIn("validate_apk(data, pin['bytes'], pin['apk_sha256'])", VERIFIER)

    def test_presence_checks_match_the_mirror_list_exactly(self):
        checked = re.findall(r'test -s site/apks/(\S+)', PAGES)
        expected: list[str] = []
        for _, asset, _, _ in MIRROR:
            expected.extend([asset, asset[:-4] + '.zip'])
        self.assertEqual(sorted(checked), sorted(expected), 'the presence checks drifted from RELEASES')
        for tag, asset, size, digest in MIRROR:
            self.assertEqual(PAGES.count(f'("{tag}", "{asset}", {size}, "{digest}")'), 1, tag)

    def test_mirror_bytes_stay_inside_the_pages_budget(self):
        budget = int(re.search(r'MIRROR_BUDGET = ([\d_]+)', PAGES).group(1).replace('_', ''))
        self.assertLess(budget, 1_000_000_000, 'the Pages upload limit is 1 GB')
        # Each release is mirrored twice: the .apk and a byte-identical stored .zip.
        mirrored = 2 * sum(int(size) for _, _, size, _ in MIRROR)
        self.assertLessEqual(mirrored, budget,
                             f'{mirrored:,} B exceeds the {budget:,} B mirror budget; prune an older mirror')
