"""Guards for the two counts that drifted without any gate noticing.

Both defects shipped in the 16-app release and were invisible to every existing check:

* ``appdisabler/app/src/main/java/com/universalrp/updates/SisterApps.kt`` is a hand-kept copy of the
  shared directory (the shared ``srcDir`` pulls in every module's sources, so the copy has to stay).
  Its System App Disabler entry was still commented out as "draft, not yet published", which meant
  the app's own About screen listed sixteen apps whose names did not include this one.
* The Store's About block printed ``Our 13 apps`` as a literal next to a fifteen-app catalog.

These tests pin the *relationships* rather than the numbers, so the next app addition updates one
place and the gates still hold.
"""
from pathlib import Path
import datetime
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / 'shared/src/main/java/com/universalrp/updates/SisterApps.kt'
LOCAL = ROOT / 'appdisabler/app/src/main/java/com/universalrp/updates/SisterApps.kt'
ABOUT = ROOT / 'appdisabler/app/src/main/java/com/universalrp/appdisabler/ui/MainActivity.kt'
STORE_MAIN = ROOT / 'tamilanstore/src/main/java/com/universalrp/tamilanstore/MainActivity.kt'
STORE_CATALOG = ROOT / 'tamilanstore/src/main/assets/catalog.json'


def entries(source):
    """id -> (version, packageName) for each active SisterApp entry."""
    found = {}
    for chunk in source.split('SisterApp(')[1:]:
        block = chunk.split('\n        ),')[0]
        app_id = re.search(r'\bid\s*=\s*"([^"]+)"', block)
        version = re.search(r'\bversion\s*=\s*"([^"]+)"', block)
        package = re.search(r'packageName\s*=\s*"([^"]+)"', block)
        if app_id and version and package:
            found[app_id.group(1)] = (version.group(1), package.group(1))
    return found


class DisablerAboutDirectoryTest(unittest.TestCase):
    def test_local_sister_apps_copy_matches_the_shared_directory(self):
        shared, local = entries(SHARED.read_text()), entries(LOCAL.read_text())
        self.assertGreaterEqual(len(shared), 16)
        self.assertEqual(shared, local,
                         'the Disabler builds against its own SisterApps copy; it must not drift')

    def test_the_disabler_lists_itself_in_its_own_about_screen(self):
        local = entries(LOCAL.read_text())
        self.assertIn('appdisabler', local)
        # The version lives in build.gradle.kts and About reads it from BuildConfig, so this
        # only guards package identity and that the app lists itself at all.
        self.assertEqual(local['appdisabler'][1], 'com.universalrp.appdisabler')
        self.assertNotIn('not yet published', LOCAL.read_text())

    def test_about_dialog_does_not_print_this_app_twice(self):
        about = ABOUT.read_text()
        # It renders the whole directory, then a dedicated "this app" line; the directory loop must
        # filter this app out or the list shows the same name twice.
        loop = re.search(r'SisterApps\.all(.*?)\.forEach \{ app ->(.*?)\n                \}',
                         about, re.S)
        self.assertIsNotNone(loop, 'the About list no longer iterates SisterApps.all')
        self.assertIn('filter { it.id != "appdisabler" }', loop.group(1))
        self.assertIn('biStatic(T.appName, taFirst)', about)


class StoreAboutCountsTest(unittest.TestCase):
    def test_app_count_is_derived_from_the_live_catalog_not_hardcoded(self):
        ui = STORE_MAIN.read_text()
        self.assertIn('${model.apps.size} apps', ui)
        self.assertIn('${model.apps.size} செயலிகள்', ui)
        # A literal count is what went stale; keep them out of the About block entirely.
        self.assertNotRegex(ui, r'Our \d+ apps')
        self.assertNotRegex(ui, r'நமது \d+ செயலிகள்')

    def test_bundled_catalog_date_matches_the_catalog_itself(self):
        updated = json.loads(STORE_CATALOG.read_text())['updated']
        stamp = datetime.date.fromisoformat(updated)
        ui = STORE_MAIN.read_text()
        self.assertIn(f'Bundled catalog: {stamp.day} {stamp.strftime("%B %Y")}', ui)
        months = ['', 'ஜனவரி', 'பிப்ரவரி', 'மார்ச்', 'ஏப்ரல்', 'மே', 'ஜூன்', 'ஜூலை',
                  'ஆகஸ்ட்', 'செப்டம்பர்', 'அக்டோபர்', 'நவம்பர்', 'டிசம்பர்']
        self.assertIn(f'{stamp.day} {months[stamp.month]} {stamp.year}', ui)

    def test_about_store_button_points_at_the_published_store_release(self):
        # The Store cannot list itself: a store that installs a store is not one of its own apps.
        catalog = json.loads(STORE_CATALOG.read_text())
        self.assertNotIn('tamilanstore', {a['id'] for a in catalog['apps']})
        sections = (ROOT / 'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        store = re.search(r'tagPrefix = "tamilan-store", version = "([^"]+)"', sections).group(1)
        ui = STORE_MAIN.read_text()
        # The Disabler's own About dialog is the one that must reach the current Store build.
        self.assertIn(f'releases/tag/tamilan-store-v{store}', ABOUT.read_text())


if __name__ == '__main__':
    unittest.main()


class DisablerSafetyGuardTest(unittest.TestCase):
    """The owner turned off 13 packages on a HyperOS phone and lost parts of Settings.

    Nothing in the app refused a package that would take away the keyboard or a live accessibility
    service, so these tests pin that refusal and the header counts that make the state visible.
    """

    MANAGER = ROOT / 'appdisabler/app/src/main/java/com/universalrp/appdisabler/data/DisableManager.kt'
    STRINGS = ROOT / 'appdisabler/app/src/main/java/com/universalrp/appdisabler/ui/Bilingual.kt'
    GRADLE = ROOT / 'appdisabler/app/build.gradle.kts'

    def test_disable_refuses_the_keyboard_in_use_and_live_accessibility_services(self):
        manager = self.MANAGER.read_text()
        guard = re.search(r'private fun blockingReason\(.*?\n    \}', manager, re.S)
        self.assertIsNotNone(guard, 'the disable guard is gone')
        body = guard.group(0)
        self.assertIn('DEFAULT_INPUT_METHOD', body,
                      'the active keyboard must be checked before disabling it')
        self.assertIn('ENABLED_ACCESSIBILITY_SERVICES', body,
                      'a live accessibility service must be checked before disabling it')
        self.assertIn('DisableCategory.CRITICAL', body, 'the never-disable list must stay')
        # The guard has to run before the first pm command, not after a failed attempt.
        disable = manager[manager.index('suspend fun disablePackage'):]
        disable = disable[:disable.index('val disableAttempts')]
        self.assertIn('blockingReason(context, packageName)', disable,
                      'the guard must fire before pm disable-user is attempted')

    def test_header_counts_are_derived_not_hardcoded(self):
        ui = ABOUT.read_text()
        self.assertIn('StatTile(', ui, 'the counts strip is missing')
        for symbol in ('📦', '⚙', '👤', '⛔', '▶'):
            self.assertIn(symbol, ui, f'missing the {symbol} tile')
        # Counts must come from the loaded list, never from a literal.
        self.assertIn('visible.count { !it.isEnabled || it.isSuspended }', ui)
        self.assertIn('visible.count { it.isSystem }', ui)
        self.assertRegex(ui, r'visible\.count \{ app -> live\.contains\(app\.packageName\) \}')

    def test_running_count_says_unknown_without_shizuku_instead_of_zero(self):
        ui = ABOUT.read_text()
        self.assertIn('runningCount?.toString() ?: "—"', ui,
                      'a missing process table must render as unknown, never as 0')
        manager = self.MANAGER.read_text()
        running = manager[manager.index('suspend fun runningProcesses'):]
        running = running[:running.index('suspend fun enablePackage')]
        self.assertIn('isShizukuAvailable()', running)
        self.assertIn('return@withContext null', running)

    def test_new_bilingual_strings_are_paired(self):
        strings = self.STRINGS.read_text()
        for key in ('statInstalled', 'statPreinstalled', 'statUser', 'statOff', 'statRunning',
                    'statRunningNeedsShizuku', 'blockedTitle', 'blockedRecovery'):
            self.assertRegex(strings, rf'{key}\s*=\s*Bi\(\s*"[^"]+"\s*,\s*"[^"]+"\s*\)',
                             f'{key} must be a Bi(english, tamil) pair')

    def test_about_reads_the_installed_version_from_build_config(self):
        ui = ABOUT.read_text()
        self.assertIn('v${BuildConfig.VERSION_NAME}', ui,
                      'About must not pin a literal version again')
        self.assertIn('buildConfig = true', self.GRADLE.read_text())
        gradle = self.GRADLE.read_text()
        code = int(re.search(r'versionCode\s*=\s*(\d+)', gradle).group(1))
        name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle).group(1)
        self.assertEqual(name, '0.2.0', 'this test was written for the 0.2.0 candidate')
        self.assertEqual(code, 2)
