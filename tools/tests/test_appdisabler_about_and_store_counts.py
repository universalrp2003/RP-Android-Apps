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
        disable = disable[:disable.index('val disable = runShell')]
        self.assertIn('blockingReason(context, packageName)', disable,
                      'the guard must fire before pm disable-user is attempted')

    def test_header_counts_are_derived_not_hardcoded(self):
        ui = ABOUT.read_text()
        self.assertIn('StatTile(', ui, 'the counts strip is missing')
        for symbol in ('📦', '⚙', '👤', '⛔', '▶'):
            self.assertIn(symbol, ui, f'missing the {symbol} tile')
        # Counts must come from the loaded list, never from a literal.
        self.assertIn('visible.count { !it.isEnabled }', ui)
        self.assertIn('visible.count { it.isSuspended }', ui)
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
        # The version lives in gradle only; this pins that it moves forward deliberately and that
        # code and name stay in step, rather than pinning one release's numbers forever.
        self.assertRegex(name, r'^\d+\.\d+\.\d+$')
        self.assertEqual(code, int(name.split('.')[1]),
                         'versionCode must track the minor version (0.2.0 -> 2, 0.3.0 -> 3)')


class BootloopDetectionTest(unittest.TestCase):
    """Red rows must come from the database's own words, and only from real warnings.

    The detector is reimplemented here against the same JSON asset so a change to the Kotlin regex
    that starts blocking safe apps (or stops blocking htmlviewer) fails this suite.
    """

    NEGATION = re.compile(r"\b(no|not|never|does not|doesn't)\b(?:(?!\bbut\b)[^.!?]){0,40}\bboot-?\s?loop", re.I)
    SECOND_HAND = re.compile(r"(apparently|some people said|others said|may|might|probably|would|i didn't test)", re.I)

    @classmethod
    def setUpClass(cls):
        cls.db = json.loads(
            (ROOT / 'appdisabler/app/src/main/assets/uad_lists.json').read_text()
        )

    @classmethod
    def risk(cls, description):
        if not description.strip():
            return False
        sentences = re.split(r'(?<=[.!?])\s+', description)
        if not any('bootloop' in s.lower() or 'boot loop' in s.lower() for s in sentences):
            return False
        negated = any(
            'bootloop' in s.lower() and cls.NEGATION.search(s) and not cls.SECOND_HAND.search(s)
            for s in sentences
        )
        return not negated

    def test_a_caution_rated_package_with_a_bootloop_warning_is_caught(self):
        # The bug this guards: htmlviewer is CAUTION, so the category alone lets it through on a
        # HyperOS phone, which is exactly the MIUI family the warning names.
        htmlviewer = self.db['com.android.htmlviewer']
        self.assertEqual(htmlviewer['category'], 'CAUTION')
        self.assertTrue(self.risk(htmlviewer['description']))

    def test_reassurances_do_not_read_as_warnings(self):
        for package in ('com.motorola.coresettingsext', 'com.miui.global.packageinstaller'):
            self.assertFalse(self.risk(self.db[package]['description']),
                             f'{package} explicitly says it does not bootloop')

    def test_a_warning_after_a_contrasting_clause_still_counts(self):
        # "you can still install APKs without it … but … could bootloop your device if removed"
        self.assertTrue(self.risk(self.db['com.google.android.packageinstaller']['description']))

    def test_the_two_packages_the_owner_turned_off_are_not_bootloop_risks(self):
        for package in ('com.milink.service', 'com.android.printspooler'):
            self.assertFalse(self.risk(self.db[package]['description']))

    def test_the_detector_fires_on_a_meaningful_number_of_entries(self):
        flagged = [k for k, v in self.db.items() if self.risk(v['description'])]
        self.assertGreaterEqual(len(flagged), 20)
        self.assertLess(len(flagged), 60, 'a wild detector would block most of the database')

    def test_every_flagged_band_and_severity_reaches_the_database_class(self):
        source = (ROOT / 'appdisabler/app/src/main/java/com/universalrp/appdisabler/data/UadDatabase.kt').read_text()
        self.assertIn('claimsBootloopRisk', source)
        self.assertIn('bootloopRisk = claimsBootloopRisk(', source)
        manager = (ROOT / 'appdisabler/app/src/main/java/com/universalrp/appdisabler/data/DisableManager.kt').read_text()
        self.assertIn('entry?.bootloopRisk == true', manager,
                      'a bootloop warning must refuse the disable, not just colour the row')


class OwnerUiFeedbackTest(unittest.TestCase):
    """The owner's read of the 0.2.0 candidate: colours unclear, running tile dead, wasted space.

    Each assertion pins one of the five requests so a later layout tweak cannot quietly undo it.
    """

    MANAGER = ROOT / 'appdisabler/app/src/main/java/com/universalrp/appdisabler/data/DisableManager.kt'
    STRINGS = ROOT / 'appdisabler/app/src/main/java/com/universalrp/appdisabler/ui/Bilingual.kt'

    def setUp(self):
        self.ui = ABOUT.read_text()
        self.manager = self.MANAGER.read_text()

    def test_safety_text_does_not_guarantee_ota_banking_or_recovery(self):
        strings = self.STRINGS.read_text()
        self.assertNotIn('OTA and banking safe', strings)
        self.assertNotIn('OTA and banking apps stay safe', strings)
        self.assertNotIn('every disable is reversible', strings)
        self.assertIn('OTA updates and banking are not guaranteed safe', strings)
        self.assertIn('recovery is not guaranteed', strings)

    def test_shell_diagnostic_hashes_installed_base_and_discloses_debug_and_splits(self):
        manager = self.manager
        self.assertIn('java.io.File(context.applicationInfo.sourceDir)', manager)
        self.assertIn('java.security.MessageDigest.getInstance("SHA-256")', manager)
        self.assertIn('Installed base APK SHA-256: $hash', manager)
        self.assertIn('ApplicationInfo.FLAG_DEBUGGABLE', manager)
        self.assertIn('applicationInfo.splitSourceDirs?.size', manager)
        self.assertIn('DisableManager.runShellTest(context)', self.ui)

    def test_rows_carry_a_green_amber_red_rating_not_a_theme_role(self):
        # Theme primary in this app is lavender, which is why "Safe to disable" read as body text.
        for colour in ('SafeGreen = androidx.compose.ui.graphics.Color(0xFF1B7F3B)',
                       'CautionAmber = androidx.compose.ui.graphics.Color(0xFFB26A00)',
                       'DangerRed = androidx.compose.ui.graphics.Color(0xFFC62828)'):
            self.assertIn(colour, self.ui)
        row = self.ui[self.ui.index('fun AppRow('):]
        row = row[:row.index('fun AboutDialog(')]
        self.assertIn('val tint = ratingColour(app)', row)
        # The app name and its rating label must take that colour, not the default on-surface one.
        self.assertRegex(row, r'Text\(\s*app\.label,\s*style = MaterialTheme\.typography\.titleSmall,\s*color = tint')
        self.assertIn('ratingLabel(app)', row)
        self.assertIn('color = tint', row)

    def test_running_tile_and_rows_use_the_live_process_list(self):
        # Tapping ▶ used to do nothing when Shizuku was up.
        self.assertIn('runningOnly = !runningOnly', self.ui)
        self.assertIn('val matchRunning = !runningOnly', self.ui)
        row = self.ui[self.ui.index('fun AppRow('):]
        row = row[:row.index('fun AboutDialog(')]
        self.assertIn('running.contains(app.packageName)', row, 'a running app should say so on its row')

    def test_search_is_collapsed_until_tapped(self):
        self.assertIn('var searchOpen by remember', self.ui)
        # The collapsed state must be an icon button, and closing it must clear the query.
        self.assertRegex(self.ui, r'if \(searchOpen\) \{')
        self.assertIn('searchOpen = false; query = ""', self.ui)

    def test_the_apps_window_no_longer_prints_tamil(self):
        # The owner asked for English here; the About dialog keeps its Tamil section.
        window = self.ui[self.ui.index('fun AppsListWindow('):self.ui.index('fun AboutDialog(')]
        self.assertNotIn('நீக்க', window)
        self.assertNotIn('முடக்க', window)
        self.assertNotIn('பட்டியலில்', window)
        self.assertNotIn('biStatic(T.fAny', window)
        # legend and tiles are literal English now
        self.assertIn('ColourKey(SafeGreen, "Safe")', window)
        self.assertIn('label = "installed"', window)

    def test_status_and_detail_labels_are_english_literals(self):
        self.assertIn('"● running now"', self.ui)
        self.assertIn('"Blocked"', self.ui)

    def test_disabled_and_suspended_states_precede_running_status(self):
        disabled = self.ui.index('!app.isEnabled -> "● disabled — tap Enable to restore"')
        suspended = self.ui.index('app.isSuspended -> "● suspended — may still run"')
        running = self.ui.index('-> "● running now"')
        stopped = self.ui.index('app.isStopped -> "● marked stopped by Android"')
        self.assertLess(disabled, suspended)
        self.assertLess(suspended, running)
        self.assertLess(running, stopped)

    def test_suspension_uses_public_api_not_enabled_lists_or_dumpsys_guess(self):
        manager = self.manager
        state = manager.split('private fun suspensionState')[1].split('private fun enabledState')[0]
        self.assertIn('isPackageSuspended(pkg)', state)
        self.assertIn('return null', state)
        self.assertNotIn('dumpsys', state)
        self.assertIn('PackageCommandVerification.matchesSuspension', manager)

    def test_persistent_flag_does_not_prove_a_process_is_running(self):
        self.assertIn('isPersistent = (ai.flags and android.content.pm.ApplicationInfo.FLAG_PERSISTENT) != 0', self.manager)
        self.assertIn('app.isPersistent && app.isSuspended -> T.happenPersistent', self.ui)
        self.assertLess(self.ui.index('!app.isEnabled -> T.happenDisabled'),
                        self.ui.index('app.isPersistent && app.isSuspended -> T.happenPersistent'))
        strings = self.STRINGS.read_text()
        self.assertIn('may still ', strings)
        self.assertNotIn('Android restarts it the moment it stops', strings)
        self.assertNotIn('Nothing breaks by trying', strings)
        self.assertNotIn('cannot start again on its own', strings)

    def test_live_suspension_is_not_inferred_from_the_local_note(self):
        self.assertIn('isSuspended = suspendedState == true', self.manager)
        self.assertIn('isSuspensionKnown = suspendedState != null', self.manager)
        self.assertIn('isSuspensionRemembered = pkg.packageName in suspended', self.manager)
        self.assertIn('suspension unknown — Enable to recover', self.ui)

    def test_actions_verify_state_and_preserve_restore(self):
        suspend_block = self.manager.split('val suspendCmd')[1].split('/** Read-only: why')[0]
        self.assertIn('s.exit, suspensionState(context, packageName), s.output, packageName, expected = true', suspend_block)
        self.assertIn('setSuspended(context, packageName, true)', suspend_block)
        self.assertIn('"am", "force-stop", "--user", user, packageName', suspend_block)
        enable = self.manager.split('suspend fun enablePackage')[1]
        self.assertIn('u.exit, suspensionState(context, packageName), u.output, packageName, expected = false', enable)
        self.assertIn('enabledState(context, packageName) != true', enable)
        self.assertIn('PackageCommandVerification.shouldAttemptUnsuspend(before, remembered, enabledBefore)', enable)
        self.assertIn('ENABLED_SUSPENSION_UNKNOWN', enable)
        self.assertIn('app.needsRestore', self.ui)

    def test_unlisted_user_apps_are_neutral_and_ai_is_opt_in(self):
        self.assertIn('category = uad?.category ?: DisableCategory.UNLISTED', self.manager)
        self.assertNotIn('if (isSystem) DisableCategory.UNLISTED else DisableCategory.SAFE', self.manager)
        self.assertIn('Text("Ask Google AI"', self.ui)
        self.assertIn('https://www.google.com/search?udm=50&q=$query', self.ui)
        self.assertIn('AI answers can be wrong', self.ui)
        self.assertIn('Opening Google sends the app name and package to Google', self.ui)
        self.assertIn('DisableCategory.CAUTION, DisableCategory.UNLISTED', self.ui)
        self.assertIn('label = { Text("Unlisted"', self.ui)
        self.assertIn('!app.isEnabled && app.isSuspended -> "● disabled and suspended"', self.ui)



class PublisherPinTest(unittest.TestCase):
    """The publisher must pin the bytes the owner ran, not a rebuild that happened to be newer.

    A builder started recording a digest into the same path the probe uses, and its rebuild had a
    different size — the exact trap PUBLISHING.md forbids. The record now names the source run and
    the publisher's pins must match it.
    """

    PUBLISHER = ROOT / 'appdisabler/tools/publish_appdisabler_0_2_0.py'
    UPLOADER = ROOT / 'appdisabler/tools/upload_appdisabler_0_2_0.py'
    RECORD = ROOT / 'docs/appdisabler-0.2.0-candidate-digest.md'

    def test_publisher_pins_match_the_recorded_owner_tested_artifact(self):
        record = self.RECORD.read_text()
        source = self.PUBLISHER.read_text()
        run = re.search(r'\| run \| (\d+) \|', record).group(1)
        artifact = re.search(r'\(id (\d+)\)', record).group(1)
        apk_sha = re.search(r'\| apk sha256 \| `([a-f0-9]{64})` \|', record).group(1)
        apk_bytes = re.search(r'\| apk bytes \| (\d+) \|', record).group(1)
        self.assertIn(f'RUN_ID = {run}', source)
        self.assertIn(f'ARTIFACT_ID = {artifact}', source)
        self.assertIn(f'APK_SHA = "{apk_sha}"', source)
        self.assertIn(f'APK_BYTES = {apk_bytes}', source)

    def test_the_uploader_pins_the_same_bytes_as_the_publisher(self):
        """0.2.0 was attached and published by the uploader, so it must not drift from the record.

        Both scripts are authoritative: the publisher is the record of what the owner ran, and the
        uploader is what actually put those bytes on the release. If one is edited without the
        other, the release and the "tested" claim would stop describing the same file.
        """
        uploader = self.UPLOADER.read_text()
        for line in ('RUN_ID = 43410268620', 'ARTIFACT_ID = 14028948231',
                     'ARCHIVE_SHA = "cfb9ac11e8dd2b087944aef7de2cec3200dae6ac024385ac52a551654382135d"',
                     'ARCHIVE_BYTES = 11157191',
                     'APK_SHA = "1f653dd99a68ae5877a5ebd9c178b790e5761866a2dc7c785393d4f298b0b196"',
                     'APK_BYTES = 11651613', 'TAG = "system-app-disabler-v0.2.0"'):
            self.assertIn(line, uploader)

    def test_the_retired_publisher_cannot_be_dispatched_by_accident(self):
        retired = (ROOT / '.github/workflows/publish-appdisabler-0-2-0.yml').read_text()
        self.assertTrue(retired.startswith('# RETIRED'), 'the failed publisher must say it is retired')
        self.assertIn('finish-appdisabler-0-2-0.yml', retired)

    def test_the_tested_record_says_nothing_was_published_to_make_it(self):
        self.assertIn('Nothing was published to produce this file', self.RECORD.read_text())

    def test_a_rebuild_digest_is_written_somewhere_else(self):
        # docs/appdisabler-0.2.0-candidate-digest.md is the pin; the builder must not overwrite it.
        builder = (ROOT / '.github/workflows/appdisabler-candidate.yml').read_text()
        self.assertNotIn('docs/appdisabler-0.2.0-candidate-digest.md', builder)
        self.assertIn('docs/appdisabler-candidate-build-digest.md', builder)

    def test_publisher_refuses_a_version_the_record_does_not_name(self):
        record = self.RECORD.read_text()
        source = self.PUBLISHER.read_text()
        self.assertIn("versionName | `0.2.0`", record)
        self.assertIn("versionCode | `2`", record)
        self.assertIn("versionCode='2' versionName='0.2.0'", source)
