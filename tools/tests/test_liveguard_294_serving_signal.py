"""Source contracts supplement native/JVM tests; not physical modem evidence."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'cleaner/src/main/java/com/universalrp/cleansweep'

class ServingSignalContracts(unittest.TestCase):
    def test_registered_fresh_unambiguous_only(self):
        s=(BASE/'data/ServingSignalPolicy.kt').read_text()
        for text in ('activeSimCount != 1', 'it.registered', 'ageMs in 0..MAX_CELL_AGE_MS', '60_000L'):
            self.assertIn(text,s)
        self.assertNotIn('maxBy',s)
        self.assertNotIn('sortedByDescending',s)
        self.assertNotIn('— means unavailable', (BASE/'ui/StatusPillHomeControls.kt').read_text())
    def test_existing_grant_and_visible_ui_not_overlay_location_permission(self):
        s=(BASE/'data/SubscriptionSignalReader.kt').read_text()
        for text in ('!ForegroundSignalAccess.visible', '!MobileNet.hasPreciseLocation(context)',
                     'if (!cell.isRegistered)', 'ServingSignalPolicy.ageMs(sampledAt, nowNanos, nowMs)', 'catch (_: SecurityException)'):
            self.assertIn(text,s)
        for text in ('requestPermissions', 'requestCellInfoUpdate', 'SharedPreferences', 'cellIdentity', 'Thread(', 'Handler('):
            self.assertNotIn(text,s)
        a=(BASE/'MainActivity.kt').read_text()
        self.assertIn('ForegroundSignalAccess.start()',a);self.assertIn('ForegroundSignalAccess.stop()',a)
    def test_home_and_mobile_do_not_fabricate_missing_signal(self):
        h=(BASE/'ui/HomeScreen.kt').read_text().split('private fun HomeMobileSignal',1)[1].split('@Composable',1)[0]
        m=(BASE/'ui/MobileScreen.kt').read_text().split('private fun MobileStatusCard',1)[1].split('private fun com.',1)[0]
        for s in (h,m):
            self.assertIn('if (percent != null)',s)
            self.assertIn('ServingSignalPolicy.displayText',s)
            for text in ('— %', '0/4', 'SignalBars(', 'mobile.quality', 'maxOrNull'):
                self.assertNotIn(text,s)
    def test_unavailable_usage_and_unconfigured_plan_are_not_zero_balance(self):
        s=(BASE/'ui/HomeScreen.kt').read_text().split('private fun HomeMobileDataCard',1)[1].split('@Composable',1)[0]
        self.assertIn('pack?.readingAvailable == true',s)
        self.assertIn('DataUsageTracker.hasConfiguredPlan',s)
        self.assertNotIn('?: tr("Not measured")',s)
    def test_no_default_data_signal_cloned_to_other_sim(self):
        s=(BASE/'data/MobileNet.kt').read_text()
        self.assertIn('if (sim.isDefaultData || sims.size == 1) signalDbm else null',s)
        self.assertIn('SubscriptionSignalReader.read(context, tm, activeSimCount, reportedCells)',s)
    def test_publisher_requires_expanded_complete_test_set(self):
        s=(ROOT/'tools/promote_verified_liveguard_294.py').read_text()
        for text in ("['required_jvm_tests'] == 902", "['required_suites'] == 86", "['required_android35_native_tests'] == 9"):
            self.assertIn(text,s)
        # The 2.94 publisher stays an immutable receipt for its own bytes; the live required-test
        # totals belong to the current release and are pinned in test_liveguard_295_corrections.py.
