from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]

class GuideChecks(unittest.TestCase):
    def test_both_guides_include_safety_topics(self):
        app = (ROOT/'weeklymoney/app/src/main/java/com/universalrp/weeklymoney/TamilGuide.kt').read_text()
        online = (ROOT/'site/weekly-money-guide.html').read_text()
        for text in (app, online):
            for term in ('Master password', '10 எழுத்துகள்', 'phone தொலைந்த', 'பழைய password', 'வேறு phone', 'ஒரே நேரத்தில் ஒருவர்', 'backup', 'மீட்க', 'நிறுவனம்', 'பெயரை மாற்ற முடியாது', 'ஒரே பெயரில்', 'தனி பெயர்'):
                self.assertIn(term.lower(), text.lower())
        self.assertIn('இதையே பயன்படுத்தாதீர்கள்', app)
    def test_first_time_explainer_and_guide_wired(self):
        activity = (ROOT/'weeklymoney/app/src/main/java/com/universalrp/weeklymoney/MainActivity.kt').read_text()
        self.assertIn('TamilGuide.firstStart', activity)
        self.assertIn('TamilGuide.guide', activity)
        self.assertIn('understandPasswordHelp', activity)
        self.assertIn('firstPasswordHelpSeen', activity)

    def test_setup_and_main_owner_name_are_visible_only_after_unlock(self):
        activity = (ROOT/'weeklymoney/app/src/main/java/com/universalrp/weeklymoney/MainActivity.kt').read_text()
        self.assertIn('notebookOwnerName', activity)
        self.assertIn('notebook.owner.name', activity)
        self.assertIn('ownerNameHelp', activity)
        self.assertIn('original owner or business name is restored', activity)
