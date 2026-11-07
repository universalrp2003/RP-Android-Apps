"""2.95 source contracts: compact pill, honest serving cell, steadier startup.

Source/JVM/emulator contracts only — no physical dual-SIM, OEM-modem, audio or Play Protect proof.
"""
from pathlib import Path
import runpy
import unittest

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'cleaner/src/main/java/com/universalrp/cleansweep'


class CompactPillTests(unittest.TestCase):
    def test_pill_text_is_only_a_coloured_percentage(self):
        s = (BASE / 'notify/PillSignalPolicy.kt').read_text()
        self.assertIn('Label("$percent%", SimSignalPolicy.tone(percent))', s)
        self.assertNotIn('S${it.slot}', s)
        self.assertNotIn('≈$', s)
        self.assertNotIn('"S"', s)
        # Slot selection and the three saved modes are untouched.
        for text in ('enum class Mode { FIRST, BOTH, OFF }', 'distinctBy { it.slot }', 'sortedBy { it.slot }',
                     'if (mode == Mode.OFF) return emptyList()'):
            self.assertIn(text, s)

    def test_approximation_is_explained_in_the_guide_not_the_pill(self):
        guide = (ROOT / 'shared/src/main/java/com/universalrp/updates/AppGuides.kt').read_text()
        self.assertIn('rough estimate of where that subscription', guide)
        self.assertIn('not reception reliability', guide)
        self.assertIn('தோராய', guide)
        self.assertNotIn('S1/S2 are physical slots', guide)
        controls = (BASE / 'ui/StatusPillHomeControls.kt').read_text()
        self.assertIn('rough estimate', controls)
        self.assertIn('தோராயம்', controls)

    def test_native_overlay_asserts_compact_percentages(self):
        s = (ROOT / 'cleaner/src/androidTest/java/com/universalrp/cleansweep/PillDefaultsNativeTest.kt').read_text()
        self.assertIn('assertFalse(text.toString().contains("≈"))', s)
        self.assertNotIn('"S1 ≈47%"', s)


class SteadyStartupTests(unittest.TestCase):
    def test_automatic_news_fetch_waits_one_minute(self):
        s = (BASE / 'data/NewsStartupPolicy.kt').read_text()
        self.assertIn('const val AUTO_FETCH_DELAY_MS = 60_000L', s)
        vm = (BASE / 'MainViewModel.kt').read_text()
        self.assertIn('NewsStartupPolicy.allowNetwork(force, userInitiated, elapsed)', vm)
        self.assertIn('allowNetwork = allowNetwork', vm)

    def test_repository_holds_the_network_not_just_the_ui(self):
        s = (BASE / 'data/TamilInfoStripRepo.kt').read_text()
        self.assertIn('allowNetwork: Boolean = true', s)
        self.assertIn('if (!allowNetwork) {', s)
        self.assertIn('No socket, no attempt marker, no feed work during startup.', s)
        # The cache-only return must come before the freshness/interval network block.
        self.assertLess(s.index('if (!allowNetwork) {'), s.index('val connectivity = context.getSystemService'))

    def test_manual_read_and_refresh_stay_available(self):
        h = (BASE / 'ui/HomeScreen.kt').read_text()
        self.assertIn('vm.loadTamilInfoStrip(force = true)', h)
        vm = (BASE / 'MainViewModel.kt').read_text()
        self.assertIn('loadTamilInfoStrip(userInitiated = true)', vm)
        self.assertIn('if (newsJob?.isActive == true) return', vm)

    def test_home_has_no_running_glow_animation_above_the_news_card(self):
        h = (BASE / 'ui/HomeScreen.kt').read_text()
        self.assertNotIn('LedBar(', h)
        self.assertIn('HorizontalDivider(', h)

    def test_heavy_readings_are_rate_limited_by_one_policy(self):
        policy = (BASE / 'ui/HomeRefreshPolicy.kt').read_text()
        for text in ('HEALTH_INTERVAL_MS = 30_000L', 'NETWORK_INTERVAL_MS = 60_000L', 'TICK_MS = 15_000L'):
            self.assertIn(text, policy)
        vm = (BASE / 'MainViewModel.kt').read_text()
        self.assertIn('HomeRefreshPolicy.due(now, lastHealthReadMs, HomeRefreshPolicy.HEALTH_INTERVAL_MS)', vm)
        self.assertIn('HomeRefreshPolicy.due(now, lastNetworkReadMs, HomeRefreshPolicy.NETWORK_INTERVAL_MS)', vm)
        self.assertIn('if (homeReadingJob?.isActive == true) return', vm)


class ServingCellTests(unittest.TestCase):
    def test_attribution_and_privacy_gates_are_unchanged(self):
        s = (BASE / 'data/ServingSignalPolicy.kt').read_text()
        for text in ('if (activeSimCount != 1) return null', 'it.registered', 'MAX_CELL_AGE_MS = 60_000L',
                     'ageMs in 0..MAX_CELL_AGE_MS'):
            self.assertIn(text, s)
        self.assertNotIn('maxBy', s)
        self.assertNotIn('sortedByDescending', s)
        reader = (BASE / 'data/SubscriptionSignalReader.kt').read_text()
        for text in ('!ForegroundSignalAccess.visible', '!MobileNet.hasPreciseLocation(context)',
                     'if (!cell.isRegistered)', 'catch (_: SecurityException)'):
            self.assertIn(text, reader)
        for text in ('requestPermissions', 'requestCellInfoUpdate', 'cellIdentity', 'Thread(', 'Handler('):
            self.assertNotIn(text, reader)

    def test_millisecond_and_undated_samples_are_handled_not_rejected_as_stale(self):
        policy = (BASE / 'data/ServingSignalPolicy.kt').read_text()
        self.assertIn('rawTimestamp <= nowElapsedMs * 2L', policy)
        self.assertIn('rawTimestamp == Long.MAX_VALUE', policy)
        self.assertIn('undatedSample', policy)
        reader = (BASE / 'data/SubscriptionSignalReader.kt').read_text()
        self.assertIn('ServingSignalPolicy.ageMs(sampledAt, nowNanos, nowMs)', reader)
        self.assertNotIn('timestamp > 0 && timestamp <= now', reader)

    def test_sim_count_is_by_physical_slot_not_list_length(self):
        policy = (BASE / 'data/SimAttributionPolicy.kt').read_text()
        self.assertIn('slots.any { it == null } -> 2', policy)
        mobile = (BASE / 'data/MobileNet.kt').read_text()
        self.assertIn('SimAttributionPolicy.activeSimCount(subscriptionSlots)', mobile)
        self.assertIn('SubscriptionSignalReader.read(context, tm, activeSimCount, reportedCells)', mobile)
        pill = (BASE / 'notify/PillSignalReader.kt').read_text()
        self.assertIn('SimAttributionPolicy.activeSimCount(', pill)
        self.assertNotIn('read(context, tm, subscriptions.size)', pill)


class ReleaseGateTests(unittest.TestCase):
    def test_publisher_pins_the_current_complete_test_set(self):
        s = (ROOT / 'tools/promote_verified_liveguard_297.py').read_text()
        for text in ("p['required_jvm_tests'] == 940", "p['required_suites'] == 92", "== 14"):
            self.assertIn(text, s)
        expected = runpy.run_path(str(ROOT / 'tools/verify_cleaner_regressions.py'))['EXPECTED']
        self.assertEqual(940, sum(expected.values()))
        self.assertEqual(92, len(expected))
        self.assertNotIn('assembleRelease', s)

    def test_candidate_gate_keeps_installed_bytes_before_instrumentation(self):
        w = (ROOT / '.github/workflows/liveguard-295-candidate.yml').read_text()
        self.assertIn('arena/7d673ac8-rp-android-apps', w)
        self.assertIn('python3 tools/verify_liveguard_295_installed.py', w)
        self.assertLess(w.index('python3 tools/verify_liveguard_295_installed.py'),
                        w.index("bash -o pipefail -c 'python3 tools/run_liveguard_295_native.py"))
        gate = (ROOT / 'tools/verify_liveguard_295_native.py').read_text()
        self.assertIn("('OfficialStatusNativeTest',5)", gate)
        self.assertIn("('PillDefaultsNativeTest',4)", gate)
        self.assertIn('native_295_compact_pill_and_signal_recency', gate)
        self.assertNotIn('subprocess', gate)
        self.assertIn('final_installed_apk_sha256', gate)
        runner = (ROOT / 'tools/run_liveguard_295_native.py').read_text()
        self.assertIn('-Pandroid.testInstrumentationRunnerArguments.expectedCandidateSha256=', runner)
        base = (ROOT / 'cleaner/src/androidTest/java/com/universalrp/cleansweep/ExactCandidateNativeTest.kt').read_text()
        self.assertIn('@Before fun installedTargetIsTheExactArchivedCandidate()', base)
        self.assertIn('Live-Guard-2.95-signed-tested-candidate', w)
        inspect = (ROOT / 'tools/inspect_liveguard_295_candidate.py').read_text()
        self.assertIn("'2.95', 100, 'live-guard-v2.94'", inspect)
        self.assertIn('c26de0c09022ea5d05169c238f91308de367dc2e0ccb16c902699d17de34c195', inspect)

    def test_release_note_states_what_was_and_was_not_tested(self):
        note = (ROOT / 'releases/live-guard-v2.95.md').read_text()
        self.assertIn('## தமிழ்', note)
        self.assertIn('not installed on a phone', note.lower())
        self.assertIn('uninstall', note.lower())
