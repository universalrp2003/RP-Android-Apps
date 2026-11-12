from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]

class RegularVaultGroupingTest(unittest.TestCase):
    def test_daily_vault_stays_regular_and_store_is_separate(self):
        source = (ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        ids = re.findall(r'id = "([^"]+)"', source)
        # Tamilan Store stays the separate optional app; TV Guard is a regular app like the rest.
        self.assertEqual(ids[-1], 'tamilanstore')
        self.assertIn('dailyvault', ids[:-1])
        self.assertIn('tvguard', ids[:-1])
        self.assertEqual(len(ids[:-1]), 14)
        self.assertIn('$RELEASE_BASE/tamilan-store-v0.12', source)
        self.assertIn('$RELEASE_BASE/daily-vault-v0.5', source)
        site = (ROOT/'site/index.html').read_text()
        regular = re.search(r'aria-label="14 regular Android applications">(.*?)</section>', site, re.S)[1]
        optional = re.search(r'aria-label="Optional Tamilan Store">(.*?)</section>', site, re.S)[1]
        self.assertEqual(len(re.findall(r'<article class="card"[^>]*>', regular)), 14)
        self.assertIn('id="app-tv-guard"', regular)
        self.assertIn('id="app-daily-vault"', regular)
        self.assertNotIn('id="app-tamilan-store"', regular)
        self.assertNotIn('id="app-daily-vault"', optional)
        self.assertEqual(len(re.findall(r'<article class="card"[^>]*>', optional)), 1)
        self.assertIn('8842606659442a1e2b8c7d28fd2affac56bb40c0bc679acf86785f3167903d55', regular)
        readme = (ROOT/'README.md').read_text()
        self.assertEqual(len(re.findall(r'^\| \[\*\*', readme, re.M)), 14)
