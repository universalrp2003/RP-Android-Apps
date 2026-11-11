"""The store-art step must never fail silently.

On 11 Nov 2026 the APK build stopped in "Make the store icons" with nothing but
"exit code 1": the artwork step installs ImageMagick at run time, and its log was
the one file the failure report did not quote. These checks keep the reason
readable and keep every listed app's artwork present.
"""

from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]


class StoreArtContractTest(unittest.TestCase):
    def test_feature_graphic_script_fails_loudly_without_a_font(self):
        script = (ROOT / 'tools/make_feature_graphics.sh').read_text()
        # ImageMagick accepts a font FILE, so an empty `convert -list font` is not fatal
        # as long as fc-match can resolve one.
        self.assertIn('fc-match', script)
        self.assertIn('no usable font found', script)
        self.assertNotRegex(script, r'pick_font[^\n]*\|\| true\n\s*FONT_TITLE')
        # Every app with a public APK and a store listing needs a feature graphic.
        for module in ('cleaner', 'radio', 'equalizer', 'tvguard'):
            self.assertRegex(script, rf'(?m)^build {module}\s')

    def test_every_listed_app_has_an_icon_and_feature_graphic(self):
        for module in ('cleaner', 'radio', 'equalizer', 'tvguard'):
            locale_root = ROOT / module / 'fastlane' / 'metadata' / 'android'
            locales = [p.name for p in locale_root.iterdir() if p.is_dir()]
            self.assertTrue(locales, module)
            for locale in locales:
                images = locale_root / locale / 'images'
                self.assertTrue((images / 'icon.png').is_file(), f'{module}/{locale} icon')
                self.assertTrue((images / 'featureGraphic.png').is_file(), f'{module}/{locale} graphic')

    def test_failure_report_quotes_the_store_art_log(self):
        workflow = (ROOT / '.github/workflows/build-apk.yml').read_text()
        summary = workflow.split('if: failure()', 1)[1]
        self.assertIn('store-art.log', summary.split('ci-build-failure.md', 1)[0])
        self.assertIn('Store artwork', summary)

    def test_no_font_failure_can_be_silent_in_the_artwork_step(self):
        workflow = (ROOT / '.github/workflows/build-apk.yml').read_text()
        step = workflow.split('- name: Make the store icons', 1)[1].split('- name: Check Cleaner', 1)[0]
        # piped through tee, so the log survives the step's non-zero exit
        self.assertIn('tee store-art.log', step)

    def test_tvguard_store_listing_uses_the_real_new_description(self):
        short = (ROOT / 'tvguard/fastlane/metadata/android/en-US/short_description.txt').read_text()
        self.assertIn('Debloat', short)
        self.assertLessEqual(len(short.strip()), 96)
        for locale in ('en-US', 'ta-IN'):
            full = (ROOT / f'tvguard/fastlane/metadata/android/{locale}/full_description.txt').read_text()
            self.assertGreater(len(full), 1200, locale)


if __name__ == '__main__':
    unittest.main()
