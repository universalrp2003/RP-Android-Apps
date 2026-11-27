"""Regression checks for read-only recovery guidance and cautious photo classification."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / 'appdisabler/app/src/main'


class DisablerRecoveryGuidanceTests(unittest.TestCase):
    def test_recovery_guidance_is_available_before_any_change(self):
        ui = (MODULE / 'java/com/universalrp/appdisabler/ui/MainActivity.kt').read_text()
        strings = (MODULE / 'java/com/universalrp/appdisabler/ui/Bilingual.kt').read_text()
        setup = ui.split('fun ShizukuSetupWindow(', 1)[1].split('fun AppsListWindow(', 1)[0]
        confirm = ui.split('confirmApp?.let { app ->', 1)[1].split('infoApp?.let { app ->', 1)[0]
        for section in (setup, confirm):
            self.assertIn('BiText(T.safetyChecklist', section)
            self.assertIn('BiText(T.recoveryChecklist', section)
        self.assertIn('verticalScroll(rememberScrollState())', confirm)
        for phrase in ('Change one app at a time', 'camera, photos, calls',
                       'does not back up app data', 'Recovery can fail',
                       'if the phone cannot start, this app cannot help'):
            self.assertIn(phrase, strings)
        self.assertNotIn('Everything here stays reversible', strings)
        self.assertNotIn('Suspended — stopped', strings)

    def test_photo_review_downgrades_the_actual_recommended_google_photos_entry(self):
        db = json.loads((MODULE / 'assets/uad_lists.json').read_text())
        source = (MODULE / 'java/com/universalrp/appdisabler/data/UadDatabase.kt').read_text()
        self.assertEqual(db['com.google.android.apps.photos']['category'], 'SAFE')
        self.assertIn('"com.google.android.apps.photos"', source)
        self.assertIn('databaseCategory == DisableCategory.SAFE', source)
        self.assertIn('packageName in PHOTO_REVIEW_PACKAGES', source)
        self.assertNotIn('contains("gallery")', source)

    def test_signed_smoke_checks_version_without_cutting_off_the_dump(self):
        smoke = (ROOT / 'appdisabler/tools/signed_tv_candidate_smoke.sh').read_text()
        self.assertIn('> "$work/package-after-upgrade.txt"', smoke)
        self.assertIn('grep -Fq \'versionName=0.5.1\' "$work/package-after-upgrade.txt"', smoke)
        self.assertNotIn("tr -d '\\r' | grep -Fq", smoke)


if __name__ == '__main__':
    unittest.main()
