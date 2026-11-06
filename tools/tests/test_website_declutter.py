"""Owner-requested website decluttering must not erase install safety or change releases."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class WebsiteDeclutter(unittest.TestCase):
    def test_removed_prominent_duplicate_copy(self):
        s=(ROOT/'site/index.html').read_text()
        for text in ('Open-Source Android Suite · 14 apps','Fresh-install signing change',
                     'Replacement-key APKs are now available below.','Backup Mirror (InternetRadio)',
                     'Radio, SMS Guard &amp; Voice Notes help','href="#migration-warning-title"'):
            self.assertNotIn(text,s)
    def test_safety_remains_in_closed_help(self):
        s=(ROOT/'site/index.html').read_text()
        self.assertIn('<details class="trust release-status" id="signing-limitation">',s)
        for text in ('Uninstalling may erase local data.','Play Protect எச்சரித்தால் நிறுத்துங்கள்',
                     'docs/fresh-install-signing-migration.md','href="#signing-limitation"'):
            self.assertIn(text,s)
