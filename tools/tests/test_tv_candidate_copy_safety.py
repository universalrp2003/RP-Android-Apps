"""Regression checks for owner-reported TV copy defects."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]

class TvCandidateCopySafetyTest(unittest.TestCase):
    def test_security_counts_are_grammatical_at_all_counts(self):
        root = ET.parse(ROOT / 'tvguard/app/src/main/res/values/strings.xml').getroot()
        text = next(e.text for e in root.findall('string') if e.get('name') == 'security_summary_counts')
        self.assertEqual('Alerts: %1$d; warnings: %2$d. Open the linked screens to review.', text)
        for alerts, warnings in [(0, 1), (1, 1), (2, 3)]:
            rendered = text.replace('%1$d', str(alerts)).replace('%2$d', str(warnings))
            self.assertNotIn('1 alerts', rendered)
            self.assertNotIn('1 warnings', rendered)

    def test_ready_copy_does_not_guarantee_restoration(self):
        source = (ROOT / 'appdisabler/app/src/main/java/com/universalrp/appdisabler/ui/Bilingual.kt').read_text()
        copy = source.split('val bodyReady = Bi(', 1)[1].split('\n    )', 1)[0]
        self.assertIn('recovery can fail', copy)
        self.assertNotIn('Everything can be restored', copy)
        self.assertNotIn('அனைத்தையும் ஒரு தட்டில் மீட்டெடுக்கலாம்', copy)

if __name__ == '__main__':
    unittest.main()
